from fastapi import APIRouter, Depends

from ..deps import get_cliente_actual
from ..models import Cliente
from ..schemas import ClienteOut

router = APIRouter(prefix="/api/v1/clientes", tags=["clientes"])


@router.get("/me", response_model=ClienteOut)
def perfil(cliente: Cliente = Depends(get_cliente_actual)):
    return cliente
