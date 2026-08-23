package com.gigaloterias.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    static class Draw {
        String date; int[] n;
        Draw(String d, int[] x){date=d;n=x;}
    }
    
    ArrayList<Draw> draws = new ArrayList<>();
    TextView result;
    Spinner lotterySpinner;
    ArrayAdapter<String> spinnerAdapter;
    ArrayList<String> lotteryList = new ArrayList<>();
    EditText manualInput;
    Random rnd = new Random();

    static final Set<Integer> PRIMES = new HashSet<>(Arrays.asList(
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53
    ));

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        
        result = findViewById(R.getId.result);
        lotterySpinner = findViewById(R.getId.lotterySpinner);
        manualInput = findViewById(R.getId.manualInput);

        // Lista de loterías personalizable
        lotteryList.add("Tinka");
        lotteryList.add("Kábala");
        lotteryList.add("Gana Gana");

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lotteryList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if(lotterySpinner != null) lotterySpinner.setAdapter(spinnerAdapter);

        // Listeners de botones principales
        findViewById(R.getId.loadButton).setOnClickListener(v -> openCsv());
        findViewById(R.getId.analyzeButton).setOnClickListener(v -> showAnalysis());
        findViewById(R.getId.generateButton).setOnClickListener(v -> generate20());
        
        View removeBtn = findViewById(R.getId.removeLotteryButton);
        if(removeBtn != null) removeBtn.setOnClickListener(v -> removeSelectedLotteryAndHistory());

        View addManualBtn = findViewById(R.getId.addManualButton);
        if(addManualBtn != null) addManualBtn.setOnClickListener(v -> addManualDraw());

        loadAsset();
    }

    void removeSelectedLotteryAndHistory(){
        if(lotteryList.isEmpty()){
            result.setText("⚠️ No hay loterías registradas.");
            return;
        }

        int selectedPos = lotterySpinner.getSelectedItemPosition();
        String removedName = lotteryList.get(selectedPos);
        
        lotteryList.remove(selectedPos);
        spinnerAdapter.notifyDataSetChanged();

        draws.clear(); // Borra el historial en memoria

        Toast.makeText(this, "Eliminado: " + removedName, Toast.LENGTH_SHORT).show();
        result.setText("🗑️ La lotería '" + removedName + "' y su historial han sido eliminados por completo.\nCarga un nuevo CSV o ingresa datos manualmente.");
    }

    void addManualDraw(){
        if(manualInput == null) return;
        String text = manualInput.getText().toString().trim();
        if(text.isEmpty()) {
            Toast.makeText(this, "Ingresa 6 números separados por comas", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String[] parts = text.split(",");
            if(parts.length < 6) {
                Toast.makeText(this, "Deben ser exactamente 6 números", Toast.LENGTH_SHORT).show();
                return;
            }
            int[] n = new int[6];
            for(int i = 0; i < 6; i++) n[i] = Integer.parseInt(parts[i].trim());
            Arrays.sort(n);

            draws.add(new Draw("Manual", n));
            manualInput.setText("");
            result.setText("✅ Sorteo agregado exitosamente.\nTotal de sorteos: " + draws.size());
        } catch (Exception e) {
            Toast.makeText(this, "Formato incorrecto. Ejemplo: 5, 12, 18, 24, 33, 41", Toast.LENGTH_LONG).show();
        }
    }

    void loadAsset(){
        try {
            InputStream in = getAssets().open("tinka.csv");
            readCsv(new BufferedReader(new InputStreamReader(in, "ISO-8859-1")));
            result.setText("📊 Historial de Tinka cargado: " + draws.size() + " sorteos.\nPulsa ANALIZAR para ver métricas completas.");
        } catch(Exception e){ 
            result.setText("ℹ️ Sin historial inicial por defecto. Puedes cargar un CSV o ingresar datos manualmente."); 
        }
    }

    void openCsv(){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("text/*"); 
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, 77);
    }

    @Override protected void onActivityResult(int r, int c, Intent data){
        super.onActivityResult(r, c, data);
        if(r == 77 && c == RESULT_OK && data != null){
            try{
                InputStream in = getContentResolver().openInputStream(data.getData());
                readCsv(new BufferedReader(new InputStreamReader(in, "ISO-8859-1")));
                result.setText("📂 Nuevo archivo CSV cargado: " + draws.size() + " sorteos registrados.");
            } catch(Exception e){ 
                result.setText("❌ Error al procesar el CSV: " + e.getMessage()); 
            }
        }
    }

    void readCsv(BufferedReader br) throws Exception {
        draws.clear(); 
        String line;
        while((line = br.readLine()) != null){
            line = line.trim(); 
            if(line.isEmpty()) continue;
            String[] p = line.split(",");
            if(p.length < 7) continue;
            try {
                int[] n = new int[6];
                for(int i = 0; i < 6; i++) n[i] = Integer.parseInt(p[i+1].trim());
                draws.add(new Draw(p[0].trim(), n));
            } catch(Exception ignored){}
        }
    }

    int[] freq(){
        int[] f = new int[54];
        for(Draw d : draws) for(int x : d.n) if(x >= 1 && x <= 53) f[x]++;
        return f;
    }

    int[] delays(){
        int[] del = new int[54];
        Arrays.fill(del, 999);
        int size = draws.size();
        for(int n = 1; n <= 53; n++){
            for(int i = size - 1; i >= 0; i--){
                if(contains(draws.get(i).n, n)){
                    del[n] = (size - 1) - i;
                    break;
                }
            }
        }
        return del;
    }

    String last(){ return draws.isEmpty() ? "Sin datos" : Arrays.toString(draws.get(draws.size() - 1).n); }

    void showAnalysis(){
        if(draws.isEmpty()){
            result.setText("⚠️ El historial está vacío. Carga un archivo CSV o ingresa un sorteo manualmente.");
            return;
        }
        int[] f = freq();
        int[] del = delays();
        Integer[] nums = new Integer[53]; 
        for(int i = 0; i < 53; i++) nums[i] = i + 1;
        Arrays.sort(nums, (a, b) -> Integer.compare(f[b], f[a]));

        StringBuilder s = new StringBuilder();
        s.append("📈 PANEL DE ANÁLISIS GIGALOTERÍAS\n");
        s.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        s.append("Sorteos analizados: ").append(draws.size()).append("\n");
        s.append("Última combinación: ").append(last()).append("\n\n");
        
        s.append("🔥 MÁS FRECUENTES (CALIENTES):\n");
        for(int i = 0; i < 6; i++) s.append(" • Número ").append(nums[i]).append(" (").append(f[nums[i]]).append(" apariciones)\n");
        
        s.append("\n❄️ MÁS RETRASADOS (FRÍOS):\n");
        for(int i = 52; i >= 47; i--) s.append(" • Número ").append(nums[i]).append(" (Atraso: ").append(del[nums[i]]).append(" sorteos)\n");
        
        result.setText(s.toString());
    }

    boolean consecutive(int[] a){
        for(int i = 0; i < a.length - 1; i++) if(a[i] + 1 == a[i+1]) return true;
        return false;
    }

    boolean contains(int[] a, int x){
        for(int v : a) if(v == x) return true;
        return false;
    }

    boolean overlapLast(int[] combo){
        if(draws.isEmpty()) return false;
        int[] last = draws.get(draws.size() - 1).n;
        for(int x : combo) if(contains(last, x)) return true;
        return false;
    }

    double score(int[] combo, int[] f, int[] del){
        int even = 0, sum = 0, primes = 0;
        for(int x : combo){
            if(x % 2 == 0) even++;
            sum += x;
            if(PRIMES.contains(x)) primes++;
        }

        // Filtros de Balance General
        if(even < 2 || even > 4) return -100;
        if(sum < 100 || sum > 170) return -100;
        if(primes < 1 || primes > 3) return -100;
        if(consecutive(combo)) return -100;
        if(overlapLast(combo)) return -100;

        // Filtro por Posición
        if(combo[0] > 18) return -100;
        if(combo[5] < 35) return -100;

        // Ley del Tercio & Ciclos de Retraso
        double sc = 0;
        for(int x : combo){
            if(del[x] > 18) return -100; 
            sc += f[x] + (10.0 / (del[x] + 1));
        }
        return sc;
    }

    void generate20(){
        if(draws.isEmpty()){
            result.setText("⚠️ Se requiere un historial de sorteos para procesar las predicciones.");
            return;
        }
        int[] f = freq();
        int[] del = delays();
        ArrayList<int[]> valid = new ArrayList<>();

        for(int i = 0; i < 30000 && valid.size() < 20; i++){
            ArrayList<Integer> pool = new ArrayList<>();
            for(int n = 1; n <= 53; n++) pool.add(n);
            Collections.shuffle(pool, rnd);

            int[] combo = new int[6];
            for(int j = 0; j < 6; j++) combo[j] = pool.get(j);
            Arrays.sort(combo);

            if(score(combo, f, del) > 0) {
                valid.add(combo);
            }
        }

        StringBuilder s = new StringBuilder("🎰 20 JUGADAS OPTIMIZADAS (Algoritmo GigaLoterías):\n\n");
        for(int i = 0; i < valid.size(); i++){
            s.append(String.format("%02d)  ", i + 1)).append(Arrays.toString(valid.get(i))).append("\n");
        }
        result.setText(s.toString());
    }
}
