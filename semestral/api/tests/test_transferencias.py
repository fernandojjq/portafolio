"""Tests del flujo crítico: transferencias entre dos usuarios distintos."""
from tests.conftest import token_de

ORIGEN_ANA = "UTPB-0003-1290"   # saldo inicial 1000
DESTINO_LUIS = "UTPB-0005-7734" # saldo inicial 200


def test_transferencia_entre_usuarios_actualiza_ambos_saldos(client, usuarios):
    headers_ana = token_de(client, "ana@utpb.com")

    r = client.post("/api/v1/transferencias", headers=headers_ana, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": DESTINO_LUIS,
        "monto": 250.00,
        "descripcion": "Pago almuerzo",
    })
    assert r.status_code == 201
    assert r.json()["estado"] == "COMPLETADA"

    # Ana ve su débito
    cuenta_ana = client.get("/api/v1/cuentas", headers=headers_ana).json()[0]
    assert cuenta_ana["saldo"] == 750.00

    # Luis (otro usuario, otra sesión) ve su crédito
    headers_luis = token_de(client, "luis@utpb.com")
    cuenta_luis = client.get("/api/v1/cuentas", headers=headers_luis).json()[0]
    assert cuenta_luis["saldo"] == 450.00

    # Y ambos tienen el movimiento registrado con la misma referencia
    movs_ana = client.get(f"/api/v1/cuentas/{cuenta_ana['id']}/movimientos",
                          headers=headers_ana).json()
    movs_luis = client.get(f"/api/v1/cuentas/{cuenta_luis['id']}/movimientos",
                           headers=headers_luis).json()
    assert movs_ana[0]["tipo"] == "DEBITO"
    assert movs_luis[0]["tipo"] == "CREDITO"


def test_saldo_insuficiente_no_modifica_nada(client, usuarios):
    headers = token_de(client, "ana@utpb.com")

    r = client.post("/api/v1/transferencias", headers=headers, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": DESTINO_LUIS,
        "monto": 99999.00,
    })
    assert r.status_code == 422
    assert r.json()["codigo"] == "SALDO_INSUFICIENTE"

    # Atomicidad: el saldo de Ana quedó intacto
    cuenta = client.get("/api/v1/cuentas", headers=headers).json()[0]
    assert cuenta["saldo"] == 1000.00


def test_no_puede_debitar_cuenta_ajena(client, usuarios):
    # Luis intenta transferir DESDE la cuenta de Ana: prohibido
    headers_luis = token_de(client, "luis@utpb.com")
    r = client.post("/api/v1/transferencias", headers=headers_luis, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": DESTINO_LUIS,
        "monto": 10.00,
    })
    assert r.status_code == 422
    assert r.json()["codigo"] == "TRANSFERENCIA_INVALIDA"


def test_monto_negativo_rechazado_por_validacion(client, usuarios):
    headers = token_de(client, "ana@utpb.com")
    r = client.post("/api/v1/transferencias", headers=headers, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": DESTINO_LUIS,
        "monto": -50.00,
    })
    # Pydantic (gt=0) lo corta antes de llegar a la lógica de negocio
    assert r.status_code == 422


def test_misma_cuenta_origen_destino(client, usuarios):
    headers = token_de(client, "ana@utpb.com")
    r = client.post("/api/v1/transferencias", headers=headers, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": ORIGEN_ANA,
        "monto": 10.00,
    })
    assert r.status_code == 422
    assert r.json()["codigo"] == "TRANSFERENCIA_INVALIDA"


def test_cuenta_destino_inexistente(client, usuarios):
    headers = token_de(client, "ana@utpb.com")
    r = client.post("/api/v1/transferencias", headers=headers, json={
        "cuenta_origen": ORIGEN_ANA,
        "cuenta_destino": "UTPB-9999-0000",
        "monto": 10.00,
    })
    assert r.status_code == 404
    assert r.json()["codigo"] == "CUENTA_NO_ENCONTRADA"
