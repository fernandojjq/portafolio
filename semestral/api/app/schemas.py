"""Schemas Pydantic = contrato JSON con la app Android.

Los nombres de campos coinciden 1:1 con los DTOs de Kotlin (snake_case,
igual que @SerializedName en la app). Los montos salen como float en el JSON
(Gson los mapea a Double); internamente el API opera siempre con Decimal.
"""
from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_serializer


# ---------- Auth ----------
class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1)


class RegisterRequest(BaseModel):
    nombre: str = Field(min_length=1, max_length=80)
    apellido: str = Field(min_length=1, max_length=80)
    email: EmailStr
    cedula: str = Field(min_length=1, max_length=20)
    password: str = Field(min_length=6)
    tipo_cuenta: str = Field(pattern="^(AHORRO|CORRIENTE)$")



class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str


class RefreshRequest(BaseModel):
    refresh_token: str


# ---------- Clientes ----------
class ClienteOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)  # permite crear desde ORM

    id: int
    nombre: str
    apellido: str
    email: str


# ---------- Cuentas / Movimientos ----------
class MontoSerializerMixin(BaseModel):
    """Serializa Decimals como float para que Gson (Android) reciba números."""

    @field_serializer("saldo", "monto", "saldo_resultante", check_fields=False)
    def _decimal_a_float(self, valor: Decimal) -> float:
        return float(valor)


class CuentaOut(MontoSerializerMixin):
    model_config = ConfigDict(from_attributes=True)

    id: int
    numero_cuenta: str
    tipo: str
    saldo: Decimal
    moneda: str


class CuentaBusquedaOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    numero_cuenta: str
    nombre_titular: str


class MovimientoOut(MontoSerializerMixin):
    model_config = ConfigDict(from_attributes=True)

    id: int
    tipo: str
    monto: Decimal
    saldo_resultante: Decimal
    descripcion: str
    fecha: datetime


# ---------- Transferencias ----------
class TransferenciaRequest(BaseModel):
    cuenta_origen: str
    cuenta_destino: str
    # gt=0 rechaza montos negativos o cero ANTES de tocar la lógica de negocio
    monto: Decimal = Field(gt=0, decimal_places=2)
    descripcion: str | None = Field(default=None, max_length=200)


class TransferenciaOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    referencia: str
    estado: str
    fecha: datetime


class CrearCuentaRequest(BaseModel):
    tipo_cuenta: str = Field(pattern="^(AHORRO|CORRIENTE)$")
