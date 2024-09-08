package org.ar.pay.handler;


import org.ar.common.core.annotation.HandlerAnnotation;
import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.vo.PaymentOrderVo;
import org.springframework.util.CollectionUtils;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.ar.pay.handler.Handler;


public class HandlerChain {
    private List<Integer> offsets = new ArrayList<>();
    private final Map<Integer, Handler> handlerList = new LinkedHashMap<>();
    public HandlerChain(String packageName){
        try{
          generateInstances(packageName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public void generateInstances(String packageName) throws IOException{
        List<Class<?>> allClass = getAllClass(packageName);
        if(CollectionUtils.isEmpty(allClass)){
            return ;
        }
        offsets = allClass.stream().map(t-> t.getAnnotation(HandlerAnnotation.class).offset()).sorted().collect(Collectors.toList());
        allClass.forEach(clazz->{
            HandlerAnnotation annotation = clazz.getAnnotation(HandlerAnnotation.class);
            try{
                handlerList.put(annotation.offset(),(Handler)clazz.newInstance());
            }catch (InstantiationException e){
                e.printStackTrace();
            }catch(IllegalAccessException e){
                e.printStackTrace();
            }
        });

        handlerList.forEach((k,v)->{
            int size = offsets.size() -1;
            int index = offsets.indexOf(k);
            if(size > index){
                v.setNextHandler(handlerList.get(offsets.get(index+1)));
            }
        });
    }

    private List<Class<?>> getAllClass(String packgeName) throws IOException{
        List<Class<?>> list = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packgeName.replace('.','/');
        Enumeration<URL> resources = classLoader.getResources(path);
        while(resources.hasMoreElements()){
            File file = new File(resources.nextElement().getFile());
           File[] files = file.listFiles();

           Arrays.asList(files).forEach(f->{
               if(f.getName().endsWith(".class")){
                   try{
                       String s = f.getName();
                       String[] split = s.split("\\.");
                       list.add(Class.forName(packgeName+"."+split[0]));

                   }catch (ClassNotFoundException e){
                       e.printStackTrace();
                   }
               }
           });


        }
        return list;
    }

    public PayConfig handler(PaymentOrderVo paymentOrderVo,PayConfig payConfig, Map<String, List<BankInfo>> listMap){
      return  handlerList.get(offsets.get(0)).handler(paymentOrderVo,payConfig,listMap);

    }


}
