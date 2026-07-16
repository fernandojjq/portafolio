"""Punto de entrada del API UTPB 360.

Correr en desarrollo:
    uvicorn app.main:app --reload --port 8000
Docs interactivas (Swagger): http://localhost:8000/docs
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from .database import Base, engine
from .excepciones import ApiException
from .routers import auth, clientes, cuentas, transferencias


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Crea las tablas al ARRANCAR el servidor, no al importar el módulo:
    # así los tests pueden importar `app` sin necesitar Oracle corriendo.
    # Para el mock basta create_all; en un proyecto en evolución se usaría
    # Alembic para versionar el esquema (migraciones).
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(
    title="UTPB 360 API",
    description="API de banca en línea (mock) — United Transoceanic Power Bank",
    version="1.0.0",
    lifespan=lifespan,
)


@app.exception_handler(ApiException)
def manejar_api_exception(_: Request, exc: ApiException) -> JSONResponse:
    """Convierte cualquier error de negocio al formato {codigo, mensaje}
    que la app Android sabe parsear."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"codigo": exc.codigo, "mensaje": exc.mensaje},
    )


app.include_router(auth.router)
app.include_router(clientes.router)
app.include_router(cuentas.router)
app.include_router(transferencias.router)


@app.get("/", tags=["salud"])
def salud():
    return {"servicio": "UTPB 360 API", "estado": "OK"}
