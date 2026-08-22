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
        result=findViewById(R.id.result);
        findViewById(R.id.loadButton).setOnClickListener(v -> openCsv());
        findViewById(R.id.analyzeButton).setOnClickListener(v -> showAnalysis());
        findViewById(R.id.generateButton).setOnClickListener(v -> generate20());
        loadAsset();
    }

    void loadAsset(){
        try {
            InputStream in=getAssets().open("tinka.csv");
            readCsv(new BufferedReader(new InputStreamReader(in, "ISO-8859-1")));
            result.setText("Tinka cargada: "+draws.size()+" sorteos.\nPulsa ANALIZAR para ver estadísticas.");
        } catch(Exception e){ result.setText("No se pudo cargar el historial: "+e.getMessage()); }
    }

    void openCsv(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("text/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, 77);
    }

    @Override protected void onActivityResult(int r,int c,Intent data){
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
        for(Draw d:draws) for(int x:d.n) if(x>=1&&x<f.length) f[x]++;
        return f;
    }

    String last(){ return draws.isEmpty()?"":Arrays.toString(draws.get(draws.size()-1).n); }

    void showAnalysis(){
        if(draws.isEmpty()){result.setText("No hay historial.");return;}
        int[] f=freq();
        Integer[] nums=new Integer[53]; for(int i=0;i<53;i++) nums[i]=i+1;
        Arrays.sort(nums,(a,b)->Integer.compare(f[b],f[a]));
        StringBuilder s=new StringBuilder();
        s.append("ANÁLISIS\nSorteos: ").append(draws.size())
         .append("\nÚltimo: ").append(last()).append("\n\n");
        s.append("TOP 10 FRECUENCIA:\n");
        for(int i=0;i<10;i++) s.append(String.format("%2d = %d veces\n",nums[i],f[nums[i]]));
        s.append("\nMENOS FRECUENTES (10):\n");
        Arrays.sort(nums,(a,b)->Integer.compare(f[a],f[b]));
        for(int i=0;i<10;i++) s.append(String.format("%2d = %d veces\n",nums[i],f[nums[i]]));
        s.append("\nÚltimo sorteo: pares/impares ");
        int ev=0; for(int x:draws.get(draws.size()-1).n) if(x%2==0) ev++;
        s.append(ev).append("/").append(6-ev);
        s.append("\nConsecutivos: ").append(consecutive(draws.get(draws.size()-1).n));
        result.setText(s.toString());
    }

    int consecutive(int[] a){
        int[] x=a.clone(); Arrays.sort(x); int c=0;
        for(int i=1;i<x.length;i++) if(x[i]==x[i-1]+1)c++;
        return c;
    }

    boolean contains(int[] a,int x){for(int v:a)if(v==x)return true;return false;}

    int overlapLast(int[] a){
        if(draws.isEmpty())return 0;
        int c=0; for(int x:a) if(contains(draws.get(draws.size()-1).n,x)) c++;
        return c;
    }

    int score(int[] a,int[] f){
        // Heurística independiente: frecuencia normalizada + equilibrio par/impar
        double avg=0; for(int x:a)avg+=f[x]; avg/=a.length;
        int ev=0;for(int x:a)if(x%2==0)ev++;
        double balance=(ev==2||ev==3||ev==4)?12:0;
        int cons=consecutive(a);
        double consScore=(cons<=2)?8:0;
        double freqScore=Math.min(60,avg/(draws.size()*6.0)*100.0*6);
        double repeatPenalty=overlapLast(a)*6;
        return (int)Math.max(0,Math.min(100,freqScore+balance+consScore-repeatPenalty));
    }

    void generate20(){
        if(draws.isEmpty()){result.setText("No hay historial.");return;}
        int[] f=freq();
        ArrayList<String> out=new ArrayList<>();
        HashSet<String> seen=new HashSet<>();
        int attempts=0;
        while(out.size()<20 && attempts++<20000){
            int[] a=new int[6]; int k=0;
            while(k<6){
                int x=1+rnd.nextInt(53);
                if(!contains(a,x)) a[k++]=x;
            }
            Arrays.sort(a);
            String key=Arrays.toString(a);
            if(seen.add(key)){
                int sc=score(a,f);
                out.add(String.format("%02d) %s  | %d/100",out.size()+1,key,sc));
            }
        }
        Collections.sort(out,(x,y)->Integer.compare(Integer.parseInt(y.substring(y.indexOf("|")+1,y.indexOf("/")).trim()),
                                                      Integer.parseInt(x.substring(x.indexOf("|")+1,x.indexOf("/")).trim())));
        StringBuilder s=new StringBuilder("20 JUGADAS — MOTOR ESTADÍSTICO\n\n");
        for(String x:out)s.append(x).append("\n");
        s.append("\nNota: puntuación heurística, no predicción garantizada.");
        result.setText(s.toString());
    }
}
