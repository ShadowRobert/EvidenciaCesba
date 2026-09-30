package com.example.evidenciacesba;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // COMPONENTES DE LA INTERFAZ
    private ImageView imgFoto;
    private EditText edtTitulo;
    private EditText edtDescripcion;

    private Button btnTomarFoto;
    private Button btnGuardar;

    private TextView txtEstado;

    // NUEVO: componentes para las funciones agregadas
    private TextView txtHora;               // Nivel 1: hora de captura
    private TextView txtContador;           // Nivel 2: contador de fotografías
    private LinearLayout contenedorGaleria; // Nivel 3: galería de evidencias
    private Button btnFicha;                // Nivel 3: generar ficha

    // VARIABLE PARA SABER SI EXISTE UNA FOTO
    private Bitmap fotografiaBitmap = null;

    /*
     * NUEVO: clase pequeña que representa una foto del historial.
     * Guarda la imagen y la hora en que fue capturada.
     */
    private static class FotoEvidencia {
        Bitmap imagen;
        String hora;

        FotoEvidencia(Bitmap imagen, String hora) {
            this.imagen = imagen;
            this.hora = hora;
        }
    }

    /*
     * NUEVO: historial de todas las fotos tomadas.
     * Cuando se toma otra foto, la anterior NO se borra,
     * se queda aquí como histórico.
     */
    private final ArrayList<FotoEvidencia> historialFotos = new ArrayList<>();

    /*
     * ActivityResultLauncher
     *
     * Se encarga de recibir el resultado
     * después de utilizar la cámara.
     */
    private final ActivityResultLauncher<Intent> launcherCamara =
            registerForActivityResult(

                    new ActivityResultContracts.StartActivityForResult(),

                    resultado -> {

                        /*
                         * Verificamos si la cámara terminó
                         * correctamente.
                         */
                        if (resultado.getResultCode() == RESULT_OK) {

                            Intent datos = resultado.getData();

                            if (datos != null &&
                                    datos.getExtras() != null) {

                                /*
                                 * Recuperamos la miniatura
                                 * generada por la cámara.
                                 */
                                Bitmap nuevaFoto = (Bitmap) datos
                                        .getExtras()
                                        .get("data");

                                if (nuevaFoto != null) {

                                    fotografiaBitmap = nuevaFoto;

                                    /*
                                     * Mostramos la fotografía
                                     * en el ImageView.
                                     */
                                    imgFoto.setImageBitmap(
                                            fotografiaBitmap
                                    );

                                    // NUEVO (Nivel 1): hora de captura
                                    String hora = new SimpleDateFormat(
                                            "HH:mm:ss",
                                            Locale.getDefault()
                                    ).format(new Date());

                                    txtHora.setText(
                                            "Hora de captura: " + hora
                                    );

                                    // NUEVO (Nivel 1): la foto anterior queda en el historial
                                    historialFotos.add(
                                            new FotoEvidencia(nuevaFoto, hora)
                                    );

                                    // NUEVO (Nivel 1): el botón ahora dice "Tomar otra foto"
                                    btnTomarFoto.setText("Tomar otra foto");

                                    // NUEVO (Nivel 2 y 3): refrescar contador y galería
                                    actualizarContador();
                                    actualizarGaleria();

                                    txtEstado.setText(
                                            "Fotografía tomada correctamente"
                                    );

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Fotografía recibida",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }

                        } else {

                            txtEstado.setText(
                                    "La fotografía fue cancelada"
                            );

                            Toast.makeText(
                                    MainActivity.this,
                                    "Operación cancelada",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // CONECTAR JAVA CON XML

        imgFoto = findViewById(R.id.imgFoto);
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        txtEstado = findViewById(R.id.txtEstado);

        // NUEVO: conectar los componentes agregados
        txtHora = findViewById(R.id.txtHora);
        txtContador = findViewById(R.id.txtContador);
        contenedorGaleria = findViewById(R.id.contenedorGaleria);
        btnFicha = findViewById(R.id.btnFicha);

        // NUEVO: mostrar el contador en 0 al iniciar
        actualizarContador();

        /*
         * BOTÓN TOMAR FOTOGRAFÍA
         */
        btnTomarFoto.setOnClickListener(v -> {

            abrirCamara();

        });


        /*
         * BOTÓN GUARDAR
         */
        btnGuardar.setOnClickListener(v -> {

            guardarEvidencia();

        });

        /*
         * NUEVO: BOTÓN GENERAR FICHA
         */
        btnFicha.setOnClickListener(v -> {

            generarFicha();

        });
    }


    /*
     * MÉTODO PARA ABRIR LA CÁMARA
     */
    private void abrirCamara() {

        /*
         * Creamos un Intent.
         *
         * ACTION_IMAGE_CAPTURE indica que queremos
         * utilizar una aplicación capaz de
         * tomar fotografías.
         */
        Intent intentCamara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        /*
         * Verificamos que exista una aplicación
         * capaz de utilizar este Intent.
         */
        if (intentCamara.resolveActivity(
                getPackageManager()) != null) {

            launcherCamara.launch(
                    intentCamara
            );

        } else {

            Toast.makeText(
                    this,
                    "No se encontró una aplicación de cámara",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    /*
     * NUEVO (Nivel 2): CONTADOR DE FOTOGRAFÍAS
     */
    private void actualizarContador() {

        txtContador.setText(
                "Fotografías capturadas: " + historialFotos.size()
        );
    }


    /*
     * NUEVO (Nivel 3): GALERÍA DE EVIDENCIAS
     *
     * Dibuja una miniatura por cada foto del historial.
     * Al tocar una miniatura, se muestra en grande.
     */
    private void actualizarGaleria() {

        // Limpiamos la galería y la volvemos a dibujar completa
        contenedorGaleria.removeAllViews();

        // Tamaño de la miniatura: 100dp convertidos a píxeles
        int tamanio = (int) (100 *
                getResources().getDisplayMetrics().density);

        for (FotoEvidencia foto : historialFotos) {

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(8, 8, 8, 8);

            ImageView miniatura = new ImageView(this);
            miniatura.setImageBitmap(foto.imagen);
            miniatura.setLayoutParams(
                    new LinearLayout.LayoutParams(tamanio, tamanio)
            );
            miniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Al tocar la miniatura, se ve en grande arriba
            miniatura.setOnClickListener(v -> {
                imgFoto.setImageBitmap(foto.imagen);
                txtHora.setText("Hora de captura: " + foto.hora);
            });

            TextView horaFoto = new TextView(this);
            horaFoto.setText(foto.hora);
            horaFoto.setGravity(Gravity.CENTER);

            item.addView(miniatura);
            item.addView(horaFoto);

            contenedorGaleria.addView(item);
        }
    }


    /*
     * NUEVO (Nivel 3): GENERAR FICHA DE EVIDENCIA
     *
     * Muestra un resumen con el título, la descripción,
     * el total de fotos y la hora de cada captura.
     */
    private void generarFicha() {

        // Reutilizamos las mismas validaciones
        if (!validarEvidencia()) {
            return;
        }

        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();

        StringBuilder ficha = new StringBuilder();

        ficha.append("Título: ").append(titulo).append("\n\n");
        ficha.append("Descripción: ").append(descripcion).append("\n\n");
        ficha.append("Total de fotografías: ")
                .append(historialFotos.size()).append("\n\n");
        ficha.append("Horas de captura:\n");

        for (int i = 0; i < historialFotos.size(); i++) {
            ficha.append("  Foto ")
                    .append(i + 1)
                    .append(" - ")
                    .append(historialFotos.get(i).hora)
                    .append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Ficha de evidencia")
                .setMessage(ficha.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }


    /*
     * MÉTODO PARA VALIDAR LA EVIDENCIA
     *
     * (Las validaciones que ya tenías se movieron aquí para
     * poder usarlas tanto en Guardar como en Generar ficha.)
     * Devuelve true si todo está correcto.
     */
    private boolean validarEvidencia() {

        String titulo =
                edtTitulo
                        .getText()
                        .toString()
                        .trim();

        String descripcion =
                edtDescripcion
                        .getText()
                        .toString()
                        .trim();


        // VALIDAR TÍTULO

        if (titulo.isEmpty()) {

            edtTitulo.setError(
                    "Escribe un título"
            );

            edtTitulo.requestFocus();

            return false;
        }


        // VALIDAR DESCRIPCIÓN

        if (descripcion.isEmpty()) {

            edtDescripcion.setError(
                    "Escribe una descripción"
            );

            edtDescripcion.requestFocus();

            return false;
        }


        // VALIDAR FOTOGRAFÍA

        if (fotografiaBitmap == null) {

            Toast.makeText(
                    this,
                    "Primero debes tomar una fotografía",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        return true;
    }


    /*
     * MÉTODO PARA GUARDAR LA EVIDENCIA
     */
    private void guardarEvidencia() {

        if (!validarEvidencia()) {
            return;
        }

        /*
         * TODAS LAS VALIDACIONES
         * FUERON CORRECTAS.
         */

        txtEstado.setText(
                "Evidencia registrada correctamente"
        );


        Toast.makeText(
                this,
                "Evidencia guardada",
                Toast.LENGTH_LONG
        ).show();
    }
}