package cl.laestacion.cotiza

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
 private lateinit var db: SQLiteDatabase
 private lateinit var root: LinearLayout
 private lateinit var client: EditText
 private lateinit var detail: EditText
 private lateinit var items: EditText
 private lateinit var discount: EditText
 private lateinit var service: Spinner
 private lateinit var mode: Spinner
 private lateinit var tax: Spinner
 private var lastPdf: File? = null
 private val green = Color.rgb(35,77,53)
 private val services = arrayOf("Carpintería", "Jardinería", "Limpieza de vidrios", "Cóctel", "Cenas", "Catering", "Asado dinámico", "Asesoría fotovoltaica", "Ejecución fotovoltaica")
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  db = object: SQLiteOpenHelper(this,"cotizaciones.db",null,1) {
   override fun onCreate(d: SQLiteDatabase) { d.execSQL("CREATE TABLE quotes (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT, cliente TEXT, servicio TEXT, modalidad TEXT, descripcion TEXT, partidas TEXT, descuento REAL, iva INTEGER, total REAL)") }
   override fun onUpgrade(d: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
  }.writableDatabase
  showForm()
 }
 private fun layout(): LinearLayout { val scroll=ScrollView(this); root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,30,28,30) }; scroll.addView(root); setContentView(scroll); return root }
 private fun title(t:String) { root.addView(TextView(this).apply { text=t; textSize=23f; setTextColor(green); setPadding(0,8,0,18) }) }
 private fun field(label:String, hint:String=""):EditText { root.addView(TextView(this).apply { text=label; textSize=15f }); return EditText(this).also { it.hint=hint; root.addView(it) } }
 private fun spinner(label:String, values:Array<String>):Spinner { root.addView(TextView(this).apply { text=label }); return Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,values); root.addView(it) } }
 private fun button(label:String, action:()->Unit) { root.addView(Button(this).apply { text=label; setTextColor(Color.WHITE); setBackgroundColor(green); setOnClickListener { action() } }) }
 private fun showForm() { layout(); title("LA ESTACIÓN SERVICIOS\nNueva cotización")
  client=field("Cliente", "Nombre o razón social")
  service=spinner("Servicio",services)
  mode=spinner("Modalidad",arrayOf("Detallada","Básica"))
  detail=field("Descripción / condiciones", "Alcance, fecha, lugar y observaciones")
  items=field("Partidas: una por línea", "Descripción | cantidad | precio unitario\nEj: Mano de obra | 2 | 35000")
  items.setSingleLine(false); items.minLines=4
  discount=field("Descuento en pesos", "0"); discount.inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
  tax=spinner("IVA",arrayOf("Sin IVA","Agregar IVA 19%"))
  button("CALCULAR Y GUARDAR") { saveQuote() }
  button("VER HISTORIAL") { showHistory() }
 }
 private data class Line(val label:String,val qty:Double,val price:Double) { val subtotal get()=qty*price }
 private fun parseLines(raw:String):List<Line> { return raw.lines().filter { it.isNotBlank() }.map { s ->
   val p=s.split('|').map { it.trim() }; require(p.size==3) { "Cada partida debe tener descripción | cantidad | precio" }
   val q=p[1].replace(',','.').toDouble(); val v=p[2].replace(',','.').toDouble(); require(q>0 && v>=0) { "Cantidad debe ser positiva y precio no negativo" }; Line(p[0],q,v)
  } }
 private fun money(n:Double)=NumberFormat.getCurrencyInstance(Locale("es","CL")).format(n)
 private fun saveQuote() { try {
  require(client.text.isNotBlank()) { "Ingresa un cliente" }
  val lines=parseLines(items.text.toString()); require(lines.isNotEmpty()) { "Ingresa al menos una partida" }
  val desc=detail.text.toString(); val d=discount.text.toString().toDoubleOrNull() ?: 0.0
  val subtotal=lines.sumOf { it.subtotal }; require(d>=0 && d<=subtotal) { "Descuento fuera de rango" }
  val base=subtotal-d; val withIva=tax.selectedItemPosition==1; val total=base*(if(withIva)1.19 else 1.0)
  val cv=ContentValues().apply { put("fecha",SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date())); put("cliente",client.text.toString()); put("servicio",service.selectedItem.toString()); put("modalidad",mode.selectedItem.toString()); put("descripcion",desc); put("partidas",items.text.toString()); put("descuento",d); put("iva",if(withIva)1 else 0); put("total",total) }
  val id=db.insertOrThrow("quotes",null,cv)
  lastPdf=generatePdf(id,client.text.toString(),service.selectedItem.toString(),mode.selectedItem.toString(),desc,lines,d,withIva)
  Toast.makeText(this,"Cotización #$id guardada: ${money(total)}",Toast.LENGTH_LONG).show()
  button("COMPARTIR ÚLTIMO PDF") { sharePdf() }
 } catch(e:Exception) { Toast.makeText(this,e.message ?: "Error",Toast.LENGTH_LONG).show() } }
 private fun generatePdf(id:Long,who:String,svc:String,mod:String,desc:String,lines:List<Line>,discount:Double,iva:Boolean):File {
  val doc=PdfDocument(); val paint=Paint(Paint.ANTI_ALIAS_FLAG); val pageW=595; val pageH=842
  var pageNo=0; var page:PdfDocument.Page?=null; var y=0f
  fun startPage() { page?.let { doc.finishPage(it) }; pageNo++; page=doc.startPage(PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create()); y=55f; paint.color=green; paint.textSize=19f; paint.isFakeBoldText=true; page!!.canvas.drawText("LA ESTACIÓN SERVICIOS",40f,y,paint); y+=30; paint.isFakeBoldText=false; paint.textSize=12f; paint.color=Color.DKGRAY }
  fun line(s:String) { if(y>780) startPage(); val text=s.take(90); page!!.canvas.drawText(text,40f,y,paint); y+=20 }
  startPage(); line("COTIZACIÓN Nº $id  |  ${SimpleDateFormat("dd-MM-yyyy",Locale.getDefault()).format(Date())}")
  line("Cliente: $who"); line("Servicio: $svc"); line("Modalidad: $mod")
  desc.lines().forEach { line("Observaciones: $it") }
  line("--------------------------------------------------------------")
  if(mod=="Básica") { line("Servicio presupuestado: ${money(lines.sumOf { it.subtotal })}") }
  else { lines.forEach { line("${it.label.take(38)} | ${it.qty} x ${money(it.price)} = ${money(it.subtotal)}") } }
  val subtotal=lines.sumOf { it.subtotal }; val base=subtotal-discount
  line("--------------------------------------------------------------")
  line("Subtotal: ${money(subtotal)}"); line("Descuento: ${money(discount)}")
  line("IVA 19%: ${money(if(iva)base*.19 else 0.0)}")
  paint.isFakeBoldText=true; line("TOTAL: ${money(base*(if(iva)1.19 else 1.0))}"); paint.isFakeBoldText=false
  line(if(iva)"Valores con IVA agregado." else "Cotización sin IVA agregado; tratamiento tributario por confirmar.")
  line("Cotización referencial, sujeta a confirmación.")
  page?.let { doc.finishPage(it) }
  val dir=File(cacheDir,"pdf").apply { mkdirs() }; val out=File(dir,"LaEstacion_Cotizacion_${id}.pdf"); out.outputStream().use { doc.writeTo(it) }; doc.close(); return out
 }
 private fun sharePdf() { val file=lastPdf ?: return; val uri=FileProvider.getUriForFile(this,"$packageName.files",file)
  val intent=Intent(Intent.ACTION_SEND).apply { type="application/pdf"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
  startActivity(Intent.createChooser(intent,"Compartir cotización"))
 }
 private fun showHistory() { layout(); title("Cotizaciones guardadas")
  val cursor=db.rawQuery("SELECT id,fecha,cliente,servicio,total FROM quotes ORDER BY id DESC",null)
  cursor.use { while(it.moveToNext()) { val id=it.getLong(0); val summary="#${id} · ${it.getString(1)}\n${it.getString(2)} · ${it.getString(3)}\n${money(it.getDouble(4))}"
   button(summary) { loadQuote(id) }
  } }
  button("NUEVA COTIZACIÓN") { showForm() }
 }
 private fun loadQuote(id:Long) { val c=db.rawQuery("SELECT cliente,servicio,modalidad,descripcion,partidas,descuento,iva FROM quotes WHERE id=?",arrayOf(id.toString()))
  c.use { if(it.moveToFirst()) { val who=it.getString(0); val svc=it.getString(1); val mod=it.getString(2); val desc=it.getString(3); val raw=it.getString(4); val disc=it.getDouble(5); val iva=it.getInt(6)==1
   layout(); title("Cotización #$id")
   root.addView(TextView(this).apply { text="$who\n$svc\n$desc\n$raw"; textSize=16f })
   button("GENERAR Y COMPARTIR PDF") { lastPdf=generatePdf(id,who,svc,mod,desc,parseLines(raw),disc,iva); sharePdf() }
   button("DUPLICAR PARA EDITAR") { showForm(); client.setText(who); service.setSelection(services.indexOf(svc).coerceAtLeast(0)); mode.setSelection(if(mod=="Básica")1 else 0); detail.setText(desc); items.setText(raw); discount.setText(disc.toString()); tax.setSelection(if(iva)1 else 0) }
   button("VOLVER AL HISTORIAL") { showHistory() }
  } }
 }
}
