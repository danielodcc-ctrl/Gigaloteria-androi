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
    Random rnd = new Random();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        result=findViewById(R.getId.result);
        findViewById(R.getId.loadButton).setOnClickListener(v -> openCsv());
        findViewById(R.getId.analyzeButton).setOnClickListener(v -> showAnalysis());
        findViewById(R.getId.generateButton).setOnClickListener(v -> generate20());
        loadAsset();
    }

    void loadAsset(){
        try {
            InputStream in=getAssets().open("tinka.csv");
            readCsv(new BufferedReader(new InputStreamReader(in, "ISO-8859-1")));
            result.setText("Tinka cargada: "+draws.size()+" sorteos.\nPulsa ANALIZAR para ver estadisticas.");
        } catch(Exception e){ result.setText("No se pudo cargar el historial: "+e.getMessage()); }
    }

    void openCsv(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("text/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, 77);
    }

    @Override protected void onActivityResult(int r, int c, Intent data){
        super.onActivityResult(r,c,data);
        if(r==77 && c==RESULT_OK && data!=null){
            try{
                InputStream in=getContentResolver().openInputStream(data.getData());
                readCsv(new BufferedReader(new InputStreamReader(in, "ISO-8859-1")));
                result.setText("Historial cargado: "+draws.size()+" sorteos.");
            }catch(Exception e){result.setText("Error leyendo CSV: "+e.getMessage());}
        }
    }

    void readCsv(BufferedReader br) throws Exception{
        draws.clear(); String line;
        while((line=br.readLine())!=null){
            line=line.trim(); if(line.isEmpty()) continue;
            String[] p=line.split(",");
            if(p.length<7) continue;
            try{
                int[] n=new int[6];
                for(int i=0;i<6;i++) n[i]=Integer.parseInt(p[i+1].trim());
                draws.add(new Draw(p[0].trim(),n));
            }catch(Exception ignored){}
        }
    }

    int[] freq(){
        int[] f=new int[54];
        for(Draw d:draws) for(int x:d.n) if(x>=1&&x<=53) f[x]++;
        return f;
    }

    String last(){ return draws.isEmpty()?"":Arrays.toString(draws.get(draws.size()-1).n); }

    void showAnalysis(){
        if(draws.isEmpty()){result.setText("No hay historial.");return;}
        int[] f=freq();
        Integer[] nums=new Integer[53]; for(int i=0;i<53;i++) nums[i]=i+1;
        Arrays.sort(nums, (a,b)->Integer.compare(f[b],f[a]));
        StringBuilder s=new StringBuilder();
        s.append("ANALISIS\nSorteos: ").append(draws.size());
        s.append("\nUltimo sorteo: ").append(last());
        s.append("\n\nTop Calientes:\n");
        for(int i=0;i<10;i++) s.append(nums[i]).append(" (").append(f[nums[i]]).append(" veces) ");
        s.append("\n\nTop Frios:\n");
        for(int i=52;i>=43;i--) s.append(nums[i]).append(" (").append(f[nums[i]]).append(" veces) ");
        result.setText(s.toString());
    }

    boolean consecutive(int[] a){
        for(int i=0;i<a.length-1;i++) if(a[i]+1==a[i+1]) return true;
        return false;
    }

    boolean contains(int[] a, int x){
        for(int v:a) if(v==x) return true;
        return false;
    }

    boolean overlapLast(int[] combo){
        if(draws.isEmpty()) return false;
        int[] last=draws.get(draws.size()-1).n;
        for(int x:combo) if(contains(last, x)) return true;
        return false;
    }

    double score(int[] combo, int[] f){
        int even=0, sum=0;
        for(int x:combo){
            if(x%2==0) even++;
            sum+=x;
        }
        if(even<2 || even>4) return -100;
        if(sum<100 || sum>170) return -100;
        if(consecutive(combo)) return -100;
        if(overlapLast(combo)) return -100;

        double sc=0;
        for(int x:combo) sc += f[x];
        return sc;
    }

    void generate20(){
        if(draws.isEmpty()){result.setText("Carga el historial primero.");return;}
        int[] f=freq();
        ArrayList<int[]> valid=new ArrayList<>();

        for(int i=0;i<10000 && valid.size()<20;i++){
            ArrayList<Integer> pool=new ArrayList<>();
            for(int n=1;n<=53;n++) pool.add(n);
            Collections.shuffle(pool, rnd);

            int[] combo=new int[6];
            for(int j=0;j<6;j++) combo[j]=pool.get(j);
            Arrays.sort(combo);

            if(score(combo, f) > 0) {
                valid.add(combo);
            }
        }

        StringBuilder s=new StringBuilder("20 JUGADAS SUGERIDAS (IA):\n\n");
        for(int i=0;i<valid.size();i++){
            s.append(i+1).append(") ").append(Arrays.toString(valid.get(i))).append("\n");
        }
        result.setText(s.toString());
    }
}

