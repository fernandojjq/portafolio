"""Fixtures compartidas.

Los tests corren contra SQLite en memoria, no Oracle: son instantáneos y no
requieren Docker. El riesgo aceptado es que SQLite ignora FOR UPDATE (no hay
bloqueo real de filas); la atomicidad y la lógica de negocio sí se prueban.
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base, get_db
from app.main import app
from app.models import Cliente, Cuenta
from app.security import hash_password

# StaticPool: todas las sesiones comparten LA MISMA conexión en memoria;
# sin esto cada sesión vería una base de datos vacía distinta.
engine = create_engine(
    "sqlite://",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestSession = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


@pytest.fixture()
def db():
    Base.metadata.create_all(bind=engine)
    sesion = TestSession()
    yield sesion
    sesion.close()
    Base.metadata.drop_all(bind=engine)  # base limpia por test: sin contaminación


@pytest.fixture()
def client(db):
    # Reemplaza la dependencia get_db para que el API use el SQLite del test
    app.dependency_overrides[get_db] = lambda: db
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture()
def usuarios(db):
    """Dos clientes con cuenta, para probar transferencias entre usuarios."""
    password = hash_password("123456")
    ana = Cliente(nombre="Ana", apellido="Morales", email="ana@utpb.com",
                  cedula="8-555-1111", password_hash=password)
    luis = Cliente(nombre="Luis", apellido="Herrera", email="luis@utpb.com",
                   cedula="8-777-2222", password_hash=password)
    db.add_all([ana, luis])
    db.flush()
    db.add_all([
        Cuenta(cliente_id=ana.id, numero_cuenta="UTPB-0003-1290",
               tipo="AHORRO", saldo=1000),
        Cuenta(cliente_id=luis.id, numero_cuenta="UTPB-0005-7734",
               tipo="CORRIENTE", saldo=200),
    ])
    db.commit()
    return {"ana": ana, "luis": luis}


def token_de(client, email: str) -> dict:
    """Helper: hace login y devuelve el header Authorization listo."""
    r = client.post("/api/v1/auth/login", json={"email": email, "password": "123456"})
    assert r.status_code == 200, r.text
    return {"Authorization": f"Bearer {r.json()['access_token']}"}
