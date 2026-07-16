import java.util.*

fun main() {
    val scanner = Scanner(System.`in`)
    var opcion: Int

    // Variables para almacenar datos del pasajero
    var nombre = ""
    var apellido = ""
    var edad = 0
    var genero = "" // M para Masculino, F para Femenino
    var registroCompleto = false

    val precioBase = 20.00

    println("--- BIENVENIDO AL SISTEMA DE TRANSPORTE UTP ---")

    do {
        println("\n--- MENÚ PRINCIPAL ---")
        println("1. Registrar pasajero")
        println("2. Realizar compra del boleto")
        println("3. Salir")
        print("Seleccione una opción: ")

        opcion = scanner.nextLine().toIntOrNull() ?: 0

        when (opcion) {
            1 -> {
                // Registro de datos
                print("Ingrese nombre: ")
                nombre = scanner.nextLine()
                print("Ingrese apellido: ")
                apellido = scanner.nextLine()
                print("Ingrese edad: ")
                edad = scanner.nextLine().toIntOrNull() ?: 0
                print("Ingrese género (M/F): ")
                genero = scanner.nextLine().uppercase()

                registroCompleto = true
                println("¡Pasajero registrado con éxito!")
            }

            2 -> {
                if (!registroCompleto) {
                    println("Error: Primero debe registrar un pasajero (Opción 1).")
                } else {
                    // 1. Concatenar nombre completo
                    val nombreCompleto = "$nombre $apellido"

                    // 2. Validar Descuento
                    var porcentajeDescuento = 0.0

                    if (edad < 12) {
                        porcentajeDescuento = 0.05 // 5%
                    } else if ((genero == "F" && edad > 57) || (genero == "M" && edad > 62)) {
                        porcentajeDescuento = 0.15 // 15%
                    }

                    val descuentoCalculado = precioBase * porcentajeDescuento
                    val precioFinal = precioBase - descuentoCalculado

                    // 3. Tipo de Pago
                    println("\nMétodos de pago disponibles: Visa, Clave, Cheque, Efectivo, Transferencia")
                    print("Ingrese el tipo de pago: ")
                    val tipoPago = scanner.nextLine()

                    // 4. Generar Recibo
                    println("\n" + "-".repeat(30))
                    println("--- TRANSPORTE UTP S.A. -----")
                    println("    RUC: 01-2531-4507")
                    println("\n    TERMINAL PRINCIPAL")
                    println("\n    CLIENTE: ${nombreCompleto.uppercase()}")
                    println("    EDAD: $edad años")
                    println("    GÉNERO: $genero")
                    println("    PAGO: $tipoPago")
                    println("    ------------------------")
                    println("    COSTO BASE: B/ ${String.format("%.2f", precioBase)}")
                    println("    DESCUENTO:  B/ ${String.format("%.2f", descuentoCalculado)} (${(porcentajeDescuento * 100).toInt()}%)")
                    println("    TOTAL:      B/ ${String.format("%.2f", precioFinal)}")
                    println("\n    BUEN VIAJE!")
                    println("-".repeat(30))

                    // Reiniciar registro tras la compra para el siguiente cliente
                    registroCompleto = false
                }
            }

            3 -> println("Saliendo del sistema... ¡Tenga un buen día!")
            else -> println("Opción no válida, intente de nuevo.")
        }

    } while (opcion != 3)
}