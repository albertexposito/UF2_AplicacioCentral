package com.yoqsetioxdxd.uf3_aplicacioclient;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private Button backButton;
    private TextView descripcionMatricula;

    private MarkerOptions[] markers;
    private ObtenerDatosPosicionAutobus datosAutobus;
    private PolylineOptions plOption;
    private boolean clickable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        backButton = (Button) findViewById(R.id.btn_back);
        descripcionMatricula = (TextView) findViewById(R.id.tv_matricula);

        clickable = true;

        //Al clickar en el boton de volver
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                datosAutobus = new ObtenerDatosPosicionAutobus(MapsActivity.this, "http://192.168.120.88:8080/WebClientRest/webresources/generic/allBuses", false);
                datosAutobus.execute();

                clickable = true;

                descripcionMatricula.setText("Selecciona un autobus clickando en la descripción del marker");

                mMap.clear();

                v.setVisibility(View.INVISIBLE);

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        datosAutobus = new ObtenerDatosPosicionAutobus(MapsActivity.this, "http://192.168.120.88:8080/WebClientRest/webresources/generic/allBuses", false);
        datosAutobus.execute();

        //Cuando clickamos en un marker
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (clickable) {
                    clickable = false;
                    mMap.clear();

                    descripcionMatricula.setText(marker.getTitle());

                    datosAutobus = new ObtenerDatosPosicionAutobus(MapsActivity.this, "http://192.168.120.88:8080/WebClientRest/webresources/generic/" + marker.getTitle(), true);
                    datosAutobus.execute();

                }
            }
        });

    }

    private class ObtenerDatosPosicionAutobus extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private String mUrl;
        private Coordenada[] ultimasPosiciones;
        private boolean drawPolyline;

        public ObtenerDatosPosicionAutobus(Context context, String url, Boolean drawPolyline) {
            mContext = context;
            mUrl = url;
            this.drawPolyline = drawPolyline;
        }

        protected Boolean doInBackground(Void... params) {
            boolean exito = false;

            String resultString = getJSON(mUrl);

            JSONArray arrayBus = null;

            try {
                arrayBus = new JSONArray(resultString);

                String matricula;
                double latitud;
                double longitud;
                String hora;

                ultimasPosiciones = new Coordenada[arrayBus.length()];
                for (int i = 0; i < arrayBus.length(); i++) {

                    JSONObject posicion = arrayBus.getJSONObject(i);

                    matricula = posicion.getString("matricula");
                    latitud = posicion.getDouble("latitud");
                    longitud = posicion.getDouble("longitud");
                    hora = posicion.getString("hora");

                    ultimasPosiciones[i] = new Coordenada(matricula, latitud, longitud, hora);

                }
                exito = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return exito;
        }

        protected void onPostExecute(Boolean exito) {
            if (exito) {
                anadirMarkers(ultimasPosiciones, drawPolyline);
                if (clickable == false) {
                    backButton.setVisibility(View.VISIBLE);
                }
            }
        }

        public String getJSON(String url) {
            HttpURLConnection c = null;
            try {
                URL u = new URL(url);
                c = (HttpURLConnection) u.openConnection();
                c.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                return sb.toString();

            } catch (Exception ex) {
                return ex.toString();
            } finally {
                if (c != null) {
                    try {
                        c.disconnect();
                    } catch (Exception ex) {
                        //disconnect error
                    }
                }
            }

        }
    }

    public void anadirMarkers(Coordenada[] ultimasPosiciones, boolean drawPolyline) {

        markers = new MarkerOptions[ultimasPosiciones.length];

        if (drawPolyline) {
            plOption = new PolylineOptions();
        }

        for (int i = 0; i < ultimasPosiciones.length; i++) {
            LatLng markerPos = new LatLng(ultimasPosiciones[i].getLatitud(), ultimasPosiciones[i].getLongitud());
            markers[i] = new MarkerOptions()
                    .position(markerPos)
                    .title(ultimasPosiciones[i].getMatricula())
                    .snippet("Hora: " + ultimasPosiciones[i].getHora());

            mMap.addMarker(markers[i]);

            if (drawPolyline) {
                plOption.add(markerPos);
            }

        }

        if (drawPolyline) {
            plOption.width(5).color(Color.BLUE).geodesic(true);
            mMap.addPolyline(plOption);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markers[0].getPosition(), 3));
        }

    }

    class Coordenada {

        private String matricula;
        private double latitud;
        private double longitud;
        private String hora;

        public Coordenada(String matricula, double latitud, double longitud, String hora) {
            this.matricula = matricula;
            this.latitud = latitud;
            this.longitud = longitud;
            this.hora = hora;
        }

        public String getMatricula() {
            return matricula;
        }

        public double getLatitud() {
            return latitud;
        }

        public double getLongitud() {
            return longitud;
        }

        public String getHora() {
            return hora;
        }
    }


}
