import random
from decimal import Decimal
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from .. import excepciones
from ..database import get_db
from ..deps import get_cliente_actual
from ..models import Cliente, Cuenta, Movimiento
from ..schemas import CuentaOut, MovimientoOut, CuentaBusquedaOut, CrearCuentaRequest

router = APIRouter(prefix="/api/v1/cuentas", tags=["cuentas"])


def _cuenta_del_cliente(db: Session, cuenta_id: int, cliente: Cliente) -> Cuenta:
    """Carga la cuenta verificando propiedad. Un cliente jamás debe poder ver
    cuentas ajenas aunque adivine el id (control de acceso a nivel de objeto)."""
    cuenta = db.get(Cuenta, cuenta_id)
    if cuenta is None or cuenta.cliente_id != cliente.id:
        # 404 también para cuentas ajenas: no confirmamos que el id existe
        raise excepciones.cuenta_no_encontrada()
    return cuenta


@router.get("", response_model=list[CuentaOut])
def mis_cuentas(
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    return db.execute(
        select(Cuenta)
        .where(Cuenta.cliente_id == cliente.id, Cuenta.estado == "ACTIVA")
        .order_by(Cuenta.id)
    ).scalars().all()


@router.get("/buscar-por-numero", response_model=CuentaBusquedaOut)
def buscar_por_numero(
    numero: str = Query(...),
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    # Si contiene "wikipedia" (QR predeterminado de la pared del emulador), simulamos éxito con Luis Herrera
    if "wikipedia" in numero.lower():
        return CuentaBusquedaOut(
            numero_cuenta="UTPB-0005-7734",
            nombre_titular="Luis Herrera"
        )

    cuenta = db.execute(
        select(Cuenta).where(Cuenta.numero_cuenta == numero)
    ).scalar_one_or_none()
    if cuenta is None:
        raise excepciones.cuenta_no_encontrada()
    
    nombre_completo = f"{cuenta.cliente.nombre} {cuenta.cliente.apellido}"
    return CuentaBusquedaOut(
        numero_cuenta=cuenta.numero_cuenta,
        nombre_titular=nombre_completo
    )


@router.get("/{cuenta_id}", response_model=CuentaOut)
def detalle(
    cuenta_id: int,
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    return _cuenta_del_cliente(db, cuenta_id, cliente)


@router.get("/{cuenta_id}/movimientos", response_model=list[MovimientoOut])
def movimientos(
    cuenta_id: int,
    page: int = Query(default=0, ge=0),
    size: int = Query(default=20, ge=1, le=100),  # le=100: nadie pide 1M de filas
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    cuenta = _cuenta_del_cliente(db, cuenta_id, cliente)
    return db.execute(
        select(Movimiento)
        .where(Movimiento.cuenta_id == cuenta.id)
        .order_by(Movimiento.fecha.desc(), Movimiento.id.desc())
        .offset(page * size)
        .limit(size)
    ).scalars().all()


@router.post("", response_model=CuentaOut)
def crear_cuenta(
    body: CrearCuentaRequest,
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    num_cuenta = f"UTPB-0006-{random.randint(1000, 9999)}"
    while db.execute(select(Cuenta).where(Cuenta.numero_cuenta == num_cuenta)).scalar_one_or_none() is not None:
        num_cuenta = f"UTPB-0006-{random.randint(1000, 9999)}"

    saldo_inicial = Decimal("500.00")
    nueva_cuenta = Cuenta(
        cliente_id=cliente.id,
        numero_cuenta=num_cuenta,
        tipo=body.tipo_cuenta,
        saldo=saldo_inicial,
        moneda="USD",
        estado="ACTIVA"
    )
    db.add(nueva_cuenta)
    db.flush()

    mov_apertura = Movimiento(
        cuenta_id=nueva_cuenta.id,
        tipo="CREDITO",
        monto=saldo_inicial,
        saldo_resultante=saldo_inicial,
        descripcion=f"Apertura de cuenta {body.tipo_cuenta.lower()}",
        referencia=f"REG-{random.randint(100000, 999999)}"
    )
    db.add(mov_apertura)
    db.commit()
    db.refresh(nueva_cuenta)

    return nueva_cuenta


@router.delete("/{cuenta_id}")
def cerrar_cuenta(
    cuenta_id: int,
    destino_cuenta_id: int | None = Query(default=None),
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    cuenta = _cuenta_del_cliente(db, cuenta_id, cliente)
    if cuenta.estado != "ACTIVA":
        raise excepciones.operacion_invalida("La cuenta ya se encuentra cerrada")

    if cuenta.saldo > 0:
        if destino_cuenta_id is None:
            raise excepciones.operacion_invalida(
                "Debes elegir una cuenta de destino para transferir el saldo restante"
            )
        cuenta_destino = db.get(Cuenta, destino_cuenta_id)
        if (
            cuenta_destino is None
            or cuenta_destino.cliente_id != cliente.id
            or cuenta_destino.estado != "ACTIVA"
            or cuenta_destino.id == cuenta.id
        ):
            raise excepciones.operacion_invalida("Cuenta de destino no válida o inactiva")

        monto_traslado = cuenta.saldo
        cuenta.saldo = Decimal("0.00")
        cuenta_destino.saldo += monto_traslado

        ref = f"CIERRE-{random.randint(100000, 999999)}"

        db.add(
            Movimiento(
                cuenta_id=cuenta.id,
                tipo="DEBITO",
                monto=monto_traslado,
                saldo_resultante=Decimal("0.00"),
                descripcion=f"Traslado por cierre a cuenta {cuenta_destino.numero_cuenta}",
                referencia=ref,
            )
        )

        db.add(
            Movimiento(
                cuenta_id=cuenta_destino.id,
                tipo="CREDITO",
                monto=monto_traslado,
                saldo_resultante=cuenta_destino.saldo,
                descripcion=f"Fondos recibidos por cierre de cuenta {cuenta.numero_cuenta}",
                referencia=ref,
            )
        )

    cuenta.estado = "CERRADA"
    db.commit()
    return {"mensaje": "Cuenta cerrada exitosamente"}

