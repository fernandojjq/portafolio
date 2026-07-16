# Informe de Implementación y Detalles del Proyecto — UTPB 360

Este documento contiene el reporte de diseño, arquitectura técnica y detalles funcionales implementados en la aplicación de banca en línea **UTPB 360**.

---

## 📱 Funcionalidades Desarrolladas e Integradas

### 1. Registro con Selección de Tipo de Cuenta
* **Interfaz de Usuario**: Se diseñó una sección de tarjetas de opción múltiple premium (Ahorros / Corriente) con bordes e indicadores de selección con el color primario de la marca.
* **Flujo**: El usuario elige el tipo de cuenta durante el registro. Al pulsar "Registrarse", se envía la solicitud al backend, que registra al usuario y crea la cuenta seleccionada automáticamente con un balance de regalo de bienvenida de **`$500.00`** e inserta su respectiva transacción inicial.

### 2. Apertura de Cuenta en Línea (Usuario Activo)
* **Interfaz de Usuario**: Una tarjeta discontinua/punteada interactiva ("+ Abrir nueva cuenta") colocada al final de la lista de cuentas en el Dashboard.
* **Flujo**: Al hacer clic, despliega un diálogo emergente modal donde el usuario elige el tipo de cuenta a aperturar. Al confirmar, llama al API remoto en caliente y aprovisiona de forma instantánea la nueva cuenta con su respectiva transacción de bienvenida de **`$500.00`**.

### 3. Historial de Transacciones (Movimientos)
* **Interfaz de Usuario**: Las tarjetas del Dashboard son completamente clickeables. Al pulsar sobre cualquiera, se abre la nueva pantalla `MovimientosScreen.kt`.
* **Detalles**:
  * Lista cronológica de todos los movimientos.
  * Formato bancario profesional: **Monto del movimiento** (en **verde con signo +** para ingresos/créditos, y **rojo con signo -** para egresos/débitos) y **Saldo resultante** de la cuenta después de cada transacción.

### 4. Generación Dinámica de Código QR
* **Interfaz de Usuario**: Botón blanco `"Código QR"` en la cabecera del historial de cada cuenta.
* **Flujo**: Lanza un modal de diálogo que realiza una petición HTTP asíncrona a un generador de código QR remoto utilizando el número de la cuenta actual. Muestra el código de barra QR centrado e instructivos para compartir y recibir transferencias inmediatas de otros usuarios.

### 5. Escudo de Privacidad mediante Sensor de Proximidad
* **Interfaz de Usuario**: Escucha activa en segundo plano en el Dashboard e Historial de Movimientos.
* **Flujo**: Usando el sensor de proximidad (`Sensor.TYPE_PROXIMITY`), si la app detecta un objeto tapando el sensor o el teléfono cerca de la cara/bolsillo, entra en **Modo Privacidad** de forma automática y al instante:
  * Convierte todos los saldos mostrados en pantalla a un formato confidencial oculto (`B/. ••••`).
  * Muestra una notificación Toast flotante de advertencia.
  * Al destapar el sensor, los montos reales se vuelven a mostrar al instante sin congelamiento de UI.

### 6. Cierre de Cuentas (Eliminación) con Traslado de Fondos
* **Interfaz de Usuario**: Botón de papelera roja en la esquina de la tarjeta de cabecera.
* **Flujo**:
  * Si la cuenta a cerrar tiene un saldo mayor a `$0.00`, la app obliga al usuario a seleccionar una cuenta de destino activa de entre sus otras cuentas.
  * Al confirmar, se realiza un débito del saldo completo de la cuenta origen y un crédito correspondiente en la cuenta destino seleccionada, guardando ambos movimientos contables en la base de datos de forma transaccional.
  * La cuenta origen se marca como `"CERRADA"` en el backend y se retira de la lista activa del Dashboard de inmediato.

---

## 🛠️ Detalle Técnico y Endpoints Nuevos de la API

### Modelos y Restricciones (Base de Datos)
* **Cuenta (`cuentas`)**: Se filtró la ruta `GET /api/v1/cuentas` para devolver únicamente los registros con `estado == "ACTIVA"`. Al cerrar la cuenta, el campo `estado` pasa a `"CERRADA"`.
* **Movimientos (`movimientos`)**: La columna `referencia` de la base de datos posee una restricción `NOT NULL`. Todas las transacciones creadas (apertura, transferencias, traslados por cierre) generan y asocian una clave de referencia alfanumérica única (`REG-XXXXXX` o `CIERRE-XXXXXX`) para evitar violaciones de integridad del motor SQLite.

### Endpoints Nuevos e Interfaces
* **`POST /api/v1/cuentas`**:
  * **Payload**: `{ "tipo_cuenta": "AHORRO" | "CORRIENTE" }`
  * **Acción**: Crea una cuenta nueva activa con saldo inicial de `$500.00` y registra su respectivo movimiento de apertura.
* **`DELETE /api/v1/cuentas/{cuenta_id}`**:
  * **Query Parameter**: `destino_cuenta_id: int | None`
  * **Acción**: Cierra la cuenta y traslada el saldo completo si es mayor a cero a la cuenta activa destino.

---

## 🧪 Pruebas Unitarias del Backend (Pytest)
Se agregaron casos de prueba avanzados para asegurar el correcto funcionamiento del backend:
* **`test_crear_cuenta_nueva`**: Valida que la apertura de cuentas asigne el tipo, el saldo de bienvenida y registre la transacción inicial con su referencia.
* **`test_cerrar_cuenta`**: Crea una cuenta adicional, la cierra trasladando el saldo a la principal, y valida que:
  * El saldo de la cuenta destino se incremente de forma correcta.
  * La cuenta cerrada ya no figure en el listado de cuentas activas de la API.
  * Las transacciones cruzadas de débito y crédito queden registradas.

Todas las pruebas se ejecutan y aprueban con éxito (`19 passed in 7.65s`).
