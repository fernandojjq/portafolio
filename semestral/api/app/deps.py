"""Dependencias compartidas de FastAPI."""
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from . import excepciones
from .database import get_db
from .models import Cliente
from .security import decodificar_token

# auto_error=False para responder nuestro formato {codigo, mensaje}
# en lugar del 403 genérico de FastAPI cuando falta el header.
_bearer = HTTPBearer(auto_error=False)


def get_cliente_actual(
    credenciales: HTTPAuthorizationCredentials | None = Depends(_bearer),
    db: Session = Depends(get_db),
) -> Cliente:
    """Resuelve el JWT del header Authorization al Cliente autenticado.

    Toda ruta protegida declara `cliente: Cliente = Depends(get_cliente_actual)`
    y recibe al dueño de la sesión — imposible olvidar la validación.
    """
    if credenciales is None:
        raise excepciones.no_autorizado()

    cliente_id = decodificar_token(credenciales.credentials, tipo_esperado="access")
    if cliente_id is None:
        raise excepciones.no_autorizado()

    cliente = db.get(Cliente, cliente_id)
    if cliente is None:
        raise excepciones.no_autorizado()
    return cliente
