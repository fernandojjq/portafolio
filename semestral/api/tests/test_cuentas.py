from tests.conftest import token_de

def test_buscar_cuenta_por_numero_existente(client, usuarios):
    headers_ana = token_de(client, "ana@utpb.com")
    r = client.get("/api/v1/cuentas/buscar-por-numero?numero=UTPB-0005-7734", headers=headers_ana)
    assert r.status_code == 200
    datos = r.json()
    assert datos["numero_cuenta"] == "UTPB-0005-7734"
    assert datos["nombre_titular"] == "Luis Herrera"

def test_buscar_cuenta_por_numero_inexistente(client, usuarios):
    headers_ana = token_de(client, "ana@utpb.com")
    r = client.get("/api/v1/cuentas/buscar-por-numero?numero=UTPB-9999-9999", headers=headers_ana)
    assert r.status_code == 404
    assert r.json()["codigo"] == "CUENTA_NO_ENCONTRADA"


def test_crear_cuenta_nueva(client, usuarios):
    headers_ana = token_de(client, "ana@utpb.com")
    r = client.post("/api/v1/cuentas", json={"tipo_cuenta": "CORRIENTE"}, headers=headers_ana)
    assert r.status_code == 200
    datos = r.json()
    assert datos["tipo"] == "CORRIENTE"
    assert datos["saldo"] == 500.0

    r_list = client.get("/api/v1/cuentas", headers=headers_ana)
    assert r_list.status_code == 200
    assert len(r_list.json()) == 2


def test_cerrar_cuenta(client, usuarios, db):
    headers_ana = token_de(client, "ana@utpb.com")

    # 1. Crear una nueva cuenta
    r = client.post("/api/v1/cuentas", json={"tipo_cuenta": "CORRIENTE"}, headers=headers_ana)
    assert r.status_code == 200
    nueva_cuenta = r.json()

    # Obtener el de ahorros
    r_list = client.get("/api/v1/cuentas", headers=headers_ana)
    cuentas = r_list.json()
    ahorros = next(c for c in cuentas if c["tipo"] == "AHORRO")
    saldo_ahorros_antes = ahorros["saldo"]

    # 2. Cerrar la nueva y transferir a ahorros
    r_del = client.delete(f"/api/v1/cuentas/{nueva_cuenta['id']}?destino_cuenta_id={ahorros['id']}", headers=headers_ana)
    assert r_del.status_code == 200

    # 3. Verificar que solo queda la de ahorros
    r_list2 = client.get("/api/v1/cuentas", headers=headers_ana)
    cuentas2 = r_list2.json()
    assert len(cuentas2) == 1
    assert cuentas2[0]["id"] == ahorros["id"]

    # 4. Verificar el saldo sumado
    assert cuentas2[0]["saldo"] == saldo_ahorros_antes + 500.0
