from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import excepciones
from ..database import get_db
from ..models import Cliente, Cuenta, Movimiento
from ..schemas import LoginRequest, RefreshRequest, TokenResponse, RegisterRequest
from ..security import (
    crear_access_token,
    crear_refresh_token,
    decodificar_token,
    verificar_password,
    hash_password,
)

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)):
    cliente = db.execute(
        select(Cliente).where(Cliente.email == body.email.lower())
    ).scalar_one_or_none()

    # Mismo error si el email no existe o la contraseña falla: no revelamos
    # cuáles correos están registrados (evita enumeración de usuarios).
    if cliente is None or not verificar_password(body.password, cliente.password_hash):
        raise excepciones.credenciales_invalidas()

    return TokenResponse(
        access_token=crear_access_token(cliente.id),
        refresh_token=crear_refresh_token(cliente.id),
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(body: RefreshRequest, db: Session = Depends(get_db)):
    cliente_id = decodificar_token(body.refresh_token, tipo_esperado="refresh")
    if cliente_id is None or db.get(Cliente, cliente_id) is None:
        raise excepciones.no_autorizado()

    # Rotación: se emite también un refresh nuevo; el viejo expira solo.
    return TokenResponse(
        access_token=crear_access_token(cliente_id),
        refresh_token=crear_refresh_token(cliente_id),
    )


@router.post("/register", response_model=TokenResponse)
def register(body: RegisterRequest, db: Session = Depends(get_db)):
    import random
    from decimal import Decimal

    # 1. Validar correo duplicado
    cliente_email = db.execute(
        select(Cliente).where(Cliente.email == body.email.lower())
    ).scalar_one_or_none()
    if cliente_email is not None:
        raise excepciones.registro_duplicado("El correo electrónico ya está registrado")

    # 2. Validar cédula duplicada
    cliente_cedula = db.execute(
        select(Cliente).where(Cliente.cedula == body.cedula)
    ).scalar_one_or_none()
    if cliente_cedula is not None:
        raise excepciones.registro_duplicado("La cédula ya está registrada")

    # 3. Crear cliente
    hash_pwd = hash_password(body.password)
    nuevo_cliente = Cliente(
        nombre=body.nombre,
        apellido=body.apellido,
        email=body.email.lower(),
        cedula=body.cedula,
        password_hash=hash_pwd
    )
    db.add(nuevo_cliente)
    db.flush()

    # 4. Crear cuenta de ahorros inicial
    num_cuenta = f"UTPB-0006-{random.randint(1000, 9999)}"
    while db.execute(select(Cuenta).where(Cuenta.numero_cuenta == num_cuenta)).scalar_one_or_none() is not None:
        num_cuenta = f"UTPB-0006-{random.randint(1000, 9999)}"

    saldo_inicial = Decimal("500.00")
    nueva_cuenta = Cuenta(
        cliente_id=nuevo_cliente.id,
        numero_cuenta=num_cuenta,
        tipo=body.tipo_cuenta,
        saldo=saldo_inicial,
        moneda="USD",
        estado="ACTIVA"
    )
    db.add(nueva_cuenta)
    db.flush()

    # Movimiento inicial
    db.add(Movimiento(
        cuenta_id=nueva_cuenta.id,
        tipo="CREDITO",
        monto=saldo_inicial,
        saldo_resultante=saldo_inicial,
        descripcion="Bono de bienvenida UTPB 360",
        referencia=f"REG-{random.randint(100000, 999999)}"
    ))

    db.commit()

    return TokenResponse(
        access_token=crear_access_token(nuevo_cliente.id),
        refresh_token=crear_refresh_token(nuevo_cliente.id),
    )

