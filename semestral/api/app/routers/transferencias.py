from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..database import get_db
from ..deps import get_cliente_actual
from ..models import Cliente
from ..schemas import TransferenciaOut, TransferenciaRequest
from ..services import ejecutar_transferencia

router = APIRouter(prefix="/api/v1/transferencias", tags=["transferencias"])


@router.post("", response_model=TransferenciaOut, status_code=201)
def transferir(
    body: TransferenciaRequest,
    cliente: Cliente = Depends(get_cliente_actual),
    db: Session = Depends(get_db),
):
    # El router queda delgado a propósito: la lógica vive en services.py,
    # que es testeable sin HTTP de por medio.
    return ejecutar_transferencia(
        db=db,
        cliente=cliente,
        numero_origen=body.cuenta_origen,
        numero_destino=body.cuenta_destino,
        monto=body.monto,
        descripcion=body.descripcion,
    )
