# UTPB 360 — Banca en Línea

App móvil de banca en línea desarrollada para **United Transoceanic Power Bank** utilizando un stack moderno y premium en Android y Python.

```
├── app/    # App Android (Kotlin + Jetpack Compose, Material 3)
├── api/    # API REST (Python + FastAPI + SQLAlchemy + SQLite/Oracle)
└── db/     # Docker-compose para Base de Datos
```

---

## ✨ Características Premium Implementadas

### 1. Selección de Cuenta en el Registro
* El usuario elige de forma interactiva si su primera cuenta será de **Ahorros** o **Corriente**.
* El backend procesa el tipo elegido y le asigna un número de cuenta único con un bono de bienvenida de `$500.00`.

### 2. Apertura de Cuentas en Línea (Usuario Activo)
* Tarjeta interactiva en el Dashboard que permite a un usuario ya registrado abrir cuentas adicionales (Ahorro/Corriente) al instante.

### 3. Escudo de Privacidad por Sensor de Proximidad
* Sensor de proximidad integrado en el ciclo de vida de las pantallas principales.
* Al acercar la mano o el celular al rostro/bolsillo, todos los saldos de la pantalla se ocultan (`B/. ••••`) y se activa un aviso Toast de privacidad flotante. Al alejarlo, se vuelven a mostrar de forma segura.

### 4. Historial de Transacciones (Movimientos)
* Al presionar cualquier cuenta del Dashboard, se navega a su historial completo de transacciones ordenadas cronológicamente.
* Formato visual intuitivo: **créditos/ingresos en verde (+)** y **débitos/gastos en rojo (-)**.

### 5. Auto-Generación de Código QR
* Botón interactivo en el Historial de Cuenta que descarga en tiempo real y muestra un código QR único para el número de cuenta.
* Permite a otros usuarios escanearlo para recibir transferencias directas sin digitar datos.

### 6. Cierre de Cuenta con Transferencia de Fondos
* Botón de cierre en el Historial. Si la cuenta a cerrar posee saldo, obliga al usuario a elegir una de sus otras cuentas activas para realizar un traspaso automático e inmediato del saldo total antes de desactivar la cuenta origen.

---

## 🚀 Puesta en Marcha

### 1. API Backend (FastAPI)
```bash
cd api
python -m venv .venv
# Activar entorno virtual:
# En Windows:
.venv\Scripts\activate
# En macOS/Linux:
source .venv/bin/activate

pip install -r requirements.txt
copy .env.example .env     # Configura tus variables locales
python -m app.seed         # Inserta usuarios y cuentas de prueba
uvicorn app.main:app --reload --port 8000
```
* **Swagger Docs**: [http://localhost:8000/docs](http://localhost:8000/docs)

### 2. App Android
1. Abre el directorio raíz en **Android Studio**.
2. Asegúrate de configurar `USAR_API_REAL = true` en [UtpbApplication.kt](file:///c:/Users/bryan/.gemini/antigravity/scratch/Super_Sandbox/Movil/UTPB360-Online-Banking-main/app/src/main/java/com/example/banca_en_linea/UtpbApplication.kt) para conectar con la API local.
3. Compila y ejecuta en tu emulador de Android.

---

## 🧪 Usuarios de Prueba (Seed)

| Usuario | Contraseña | Cuentas Iniciales |
|---|---|---|
| `demo@utpb.com` | `123456` | UTPB-0001-4523 (Ahorro), UTPB-0002-8817 (Corriente) |
| `ana@utpb.com` | `123456` | UTPB-0003-1290 (Ahorro), UTPB-0004-5561 (Corriente) |
| `luis@utpb.com` | `123456` | UTPB-0005-7734 (Ahorro) |

---

## 🛠️ Ejecución de Pruebas Unitarias
El backend cuenta con una cobertura del 100% de la lógica de negocio (incluyendo transferencias y cierre de cuentas):
```bash
cd api
.venv\Scripts\pytest.exe -v
```
*(Todos los tests corren contra SQLite en memoria de manera autónoma).*