"""Configuración centralizada, leída de variables de entorno / archivo .env.

Ventaja de pydantic-settings: los valores se validan al arrancar. Si falta
algo o tiene un tipo inválido, el API falla de inmediato con un mensaje claro
en lugar de fallar a mitad de una request.
"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = (
        "oracle+oracledb://utpb:utpb2026@localhost:1521/?service_name=XEPDB1"
    )
    jwt_secret: str = "solo-para-desarrollo"
    jwt_algoritmo: str = "HS256"
    access_token_minutos: int = 15
    refresh_token_dias: int = 7


@lru_cache  # una sola instancia por proceso
def get_settings() -> Settings:
    return Settings()
