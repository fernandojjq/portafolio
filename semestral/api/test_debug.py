from fastapi.testclient import TestClient
from app.main import app
from app.database import SessionLocal
from app.models import Cliente, Cuenta

client = TestClient(app)

# Let's call the search endpoint without auth first to see if it gives 401
r = client.get("/api/v1/cuentas/buscar-por-numero?numero=UTPB-0005-7734")
print("Without auth:", r.status_code, r.text)

# Let's override auth dependency to inspect 422 details
from app.deps import get_cliente_actual
app.dependency_overrides[get_cliente_actual] = lambda: Cliente(id=1, nombre="Evaristo", apellido="Alvarez")

try:
    r = client.get("/api/v1/cuentas/buscar-por-numero?numero=UTPB-0005-7734")
    print("With mocked auth:", r.status_code, r.text)
finally:
    app.dependency_overrides.clear()

import random
unique_email = f"test_{random.randint(1000, 9999)}@utpb.com"
unique_cedula = f"8-{random.randint(100, 999)}-{random.randint(1000, 9999)}"
register_payload = {
    "nombre": "TestUser",
    "apellido": "TestLastName",
    "email": unique_email,
    "cedula": unique_cedula,
    "password": "mysecurepassword"
}
r_reg = client.post("/api/v1/auth/register", json=register_payload)
print("Register response:", r_reg.status_code, r_reg.json())

