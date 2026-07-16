"""Hash de contraseñas (bcrypt) y emisión/verificación de JWT.

Usamos bcrypt directo (no passlib, que está sin mantenimiento). bcrypt genera
y almacena el salt dentro del propio hash, por eso no guardamos salt aparte.
"""
from datetime import datetime, timedelta, timezone

import bcrypt
import jwt

from .config import get_settings


# ---------- Contraseñas ----------
def hash_password(plano: str) -> str:
    return bcrypt.hashpw(plano.encode(), bcrypt.gensalt()).decode()


def verificar_password(plano: str, hash_guardado: str) -> bool:
    try:
        return bcrypt.checkpw(plano.encode(), hash_guardado.encode())
    except ValueError:
        # hash corrupto en DB: tratamos como credencial inválida, no como 500
        return False


# ---------- JWT ----------
def _crear_token(cliente_id: int, tipo: str, expira_en: timedelta) -> str:
    s = get_settings()
    payload = {
        "sub": str(cliente_id),
        "type": tipo,  # distinguimos access/refresh para que no sean intercambiables
        "exp": datetime.now(timezone.utc) + expira_en,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, s.jwt_secret, algorithm=s.jwt_algoritmo)


def crear_access_token(cliente_id: int) -> str:
    return _crear_token(
        cliente_id, "access", timedelta(minutes=get_settings().access_token_minutos)
    )


def crear_refresh_token(cliente_id: int) -> str:
    return _crear_token(
        cliente_id, "refresh", timedelta(days=get_settings().refresh_token_dias)
    )


def decodificar_token(token: str, tipo_esperado: str) -> int | None:
    """Devuelve el cliente_id si el token es válido y del tipo correcto."""
    s = get_settings()
    try:
        payload = jwt.decode(token, s.jwt_secret, algorithms=[s.jwt_algoritmo])
    except jwt.PyJWTError:  # expirado, firma inválida, malformado…
        return None
    if payload.get("type") != tipo_esperado:
        return None
    return int(payload["sub"])
