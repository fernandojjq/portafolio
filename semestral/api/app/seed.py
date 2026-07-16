"""Datos de demostración — 3 clientes para probar transferencias entre usuarios.

Ejecutar (con Oracle corriendo):  python -m app.seed
Es idempotente: si ya hay clientes, no duplica nada.

| Usuario          | Password | Cuentas                                    |
|------------------|----------|--------------------------------------------|
| demo@utpb.com    | 123456   | UTPB-0001-4523 (ahorro), UTPB-0002-8817    |
| ana@utpb.com     | 123456   | UTPB-0003-1290 (ahorro), UTPB-0004-5561    |
| luis@utpb.com    | 123456   | UTPB-0005-7734 (corriente)                 |
"""
from decimal import Decimal

from sqlalchemy import select

from .database import Base, SessionLocal, engine
from .models import Cliente, Cuenta, Movimiento
from .security import hash_password

CLIENTES_DEMO = [
    {
        "nombre": "Evaristo", "apellido": "Álvarez",
        "email": "demo@utpb.com", "cedula": "8-901-2345",
        "cuentas": [
            ("UTPB-0001-4523", "AHORRO", Decimal("5240.75")),
            ("UTPB-0002-8817", "CORRIENTE", Decimal("1830.20")),
        ],
    },
    {
        "nombre": "Ana", "apellido": "Morales",
        "email": "ana@utpb.com", "cedula": "8-555-1111",
        "cuentas": [
            ("UTPB-0003-1290", "AHORRO", Decimal("9800.00")),
            ("UTPB-0004-5561", "CORRIENTE", Decimal("450.50")),
        ],
    },
    {
        "nombre": "Luis", "apellido": "Herrera",
        "email": "luis@utpb.com", "cedula": "8-777-2222",
        "cuentas": [
            ("UTPB-0005-7734", "CORRIENTE", Decimal("312.40")),
        ],
    },
]


def sembrar() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        if db.execute(select(Cliente)).first() is not None:
            print("Ya existen clientes; seed omitido (es idempotente).")
            return

        # El mismo hash para todos: bcrypt es lento a propósito (~100ms/hash)
        # y las 3 cuentas demo comparten la contraseña 123456.
        password = hash_password("123456")

        for datos in CLIENTES_DEMO:
            cliente = Cliente(
                nombre=datos["nombre"], apellido=datos["apellido"],
                email=datos["email"], cedula=datos["cedula"],
                password_hash=password,
            )
            db.add(cliente)
            db.flush()  # asigna cliente.id sin cerrar la transacción

            for numero, tipo, saldo in datos["cuentas"]:
                cuenta = Cuenta(
                    cliente_id=cliente.id, numero_cuenta=numero,
                    tipo=tipo, saldo=saldo,
                )
                db.add(cuenta)
                db.flush()
                # Movimiento inicial para que el historial no arranque vacío
                db.add(Movimiento(
                    cuenta_id=cuenta.id, tipo="CREDITO", monto=saldo,
                    saldo_resultante=saldo, descripcion="Depósito inicial",
                    referencia="SEED-0001",
                ))

        db.commit()
        print(f"Seed completado: {len(CLIENTES_DEMO)} clientes creados.")
    finally:
        db.close()


if __name__ == "__main__":
    sembrar()
