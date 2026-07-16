"""Lógica de negocio de transferencias — el corazón del sistema.

Regla de oro del plan: una transferencia es UNA transacción atómica que
mueve saldo y deja rastro (2 movimientos + 1 registro de transferencia).
O se persiste todo, o no se persiste nada.
"""
import uuid
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from . import excepciones
from .models import Cliente, Cuenta, Movimiento, Transferencia


def ejecutar_transferencia(
    db: Session,
    cliente: Cliente,
    numero_origen: str,
    numero_destino: str,
    monto: Decimal,
    descripcion: str | None,
) -> Transferencia:
    if numero_origen == numero_destino:
        raise excepciones.transferencia_invalida(
            "La cuenta origen y destino no pueden ser la misma"
        )

    # with_for_update() = SELECT ... FOR UPDATE: bloquea las filas hasta el
    # commit. Sin esto, dos transferencias simultáneas desde la misma cuenta
    # podrían leer el mismo saldo y sobregirar (condición de carrera clásica).
    # Se bloquea siempre en el mismo orden (por número) para evitar deadlocks
    # cuando dos transferencias van en direcciones opuestas.
    numeros_ordenados = sorted([numero_origen, numero_destino])
    cuentas: dict[str, Cuenta] = {}
    for numero in numeros_ordenados:
        cuenta = db.execute(
            select(Cuenta).where(Cuenta.numero_cuenta == numero).with_for_update()
        ).scalar_one_or_none()
        if cuenta is None:
            raise excepciones.cuenta_no_encontrada()
        cuentas[numero] = cuenta

    origen, destino = cuentas[numero_origen], cuentas[numero_destino]

    # Autorización: solo el dueño de la cuenta origen puede debitarla.
    # (El monto > 0 ya lo garantizó Pydantic con Field(gt=0).)
    if origen.cliente_id != cliente.id:
        raise excepciones.transferencia_invalida("La cuenta origen no te pertenece")
    if origen.estado != "ACTIVA" or destino.estado != "ACTIVA":
        raise excepciones.transferencia_invalida("Una de las cuentas no está activa")
    if origen.saldo < monto:
        raise excepciones.saldo_insuficiente()

    referencia = f"TRF-{uuid.uuid4().hex[:12].upper()}"
    detalle = descripcion or "Transferencia UTPB 360"

    # --- Todo lo siguiente entra en la MISMA transacción ---
    origen.saldo -= monto
    destino.saldo += monto

    db.add(Movimiento(
        cuenta_id=origen.id, tipo="DEBITO", monto=monto,
        saldo_resultante=origen.saldo, descripcion=detalle, referencia=referencia,
    ))
    db.add(Movimiento(
        cuenta_id=destino.id, tipo="CREDITO", monto=monto,
        saldo_resultante=destino.saldo, descripcion=detalle, referencia=referencia,
    ))

    transferencia = Transferencia(
        cuenta_origen_id=origen.id, cuenta_destino_id=destino.id,
        monto=monto, estado="COMPLETADA", referencia=referencia,
    )
    db.add(transferencia)

    db.commit()  # punto de atomicidad: aquí se persiste todo junto
    db.refresh(transferencia)
    return transferencia
