"""Motor y sesiones de SQLAlchemy.

Gracias a SQLAlchemy el mismo código corre contra Oracle (desarrollo) y
SQLite (tests). Si Oracle diera problemas, cambiar DATABASE_URL a Postgres
es todo lo que haría falta.
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from .config import get_settings

engine = create_engine(
    get_settings().database_url,
    pool_pre_ping=True,  # detecta conexiones muertas antes de usarlas
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)


class Base(DeclarativeBase):
    """Base declarativa de la que heredan todos los modelos."""


def get_db():
    """Dependencia FastAPI: una sesión por request, siempre cerrada al final."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
