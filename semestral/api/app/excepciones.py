"""Errores de negocio con el formato uniforme {codigo, mensaje}.

La app Android parsea exactamente este JSON (ver ApiError en Kotlin), así que
todos los errores del API deben salir por aquí — el handler está en main.py.
"""


class ApiException(Exception):
    def __init__(self, status_code: int, codigo: str, mensaje: str):
        self.status_code = status_code
        self.codigo = codigo
        self.mensaje = mensaje


# Fábricas para los errores comunes (evita typos en los códigos)
def credenciales_invalidas() -> ApiException:
    return ApiException(401, "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos")


def no_autorizado() -> ApiException:
    return ApiException(401, "NO_AUTORIZADO", "Token inválido o expirado")


def cuenta_no_encontrada() -> ApiException:
    return ApiException(404, "CUENTA_NO_ENCONTRADA", "La cuenta no existe")


def saldo_insuficiente() -> ApiException:
    return ApiException(422, "SALDO_INSUFICIENTE", "Saldo insuficiente para la operación")


def transferencia_invalida(mensaje: str) -> ApiException:
    return ApiException(422, "TRANSFERENCIA_INVALIDA", mensaje)


def registro_duplicado(mensaje: str) -> ApiException:
    return ApiException(400, "REGISTRO_DUPLICADO", mensaje)


def operacion_invalida(mensaje: str) -> ApiException:
    return ApiException(400, "OPERACION_INVALIDA", mensaje)
