# La Estación Cotiza 1.0 — proyecto Android

Aplicación Android nativa Kotlin para presupuestos de La Estación Servicios.

## Funciones
- Presupuestos básicos o detallados para 9 categorías de servicios.
- Partidas con cantidad y precio, descuento, opción sin IVA o IVA agregado del 19%.
- Base SQLite local persistente e historial de cotizaciones.
- Duplicar una cotización, generar PDF y compartir por WhatsApp u otra aplicación.
- No requiere internet para operar.

## Compilar la APK
1. Instalar Android Studio (con Android SDK 35 y JDK 17).
2. Abrir esta carpeta como proyecto Android.
3. Esperar sincronización Gradle y elegir **Build > Build APK(s)**.
4. Instalar `app/build/outputs/apk/debug/app-debug.apk` en Android.

## Limitaciones de esta primera versión
- La opción «sin IVA» no determina si corresponde exención tributaria: es una opción de presentación del presupuesto. Confirmar la tributación aplicable antes de facturar.
- Los precios son digitados manualmente; no existe catálogo ni cálculo de costos automático.
- Los PDF tienen formato básico; aún no incluyen logo gráfico, RUT, condiciones estructuradas, firma ni validez editable.
- Los datos se guardan localmente; **desinstalar la aplicación puede borrar el historial**. Falta implementar respaldo/exportación.
- Código fuente entregado sin compilación ni prueba en dispositivo; se requiere Android Studio para verificar y generar APK.
