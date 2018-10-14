/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import com.neurotec.licensing.NLicense;
import com.neurotec.plugins.NDataFileManager;
import com.neurotec.samples.util.LibraryManager;

/**
 *
 * @author pavankumar.g
 */
public class LoadNativeLibraries {

    private static boolean isLoaded = false;

    public static void load() throws Exception {
        if (!isLoaded) {
            System.out.println("================Going to Load Native Libraries===========");
            try {

                final String components = "Biometrics.FaceExtraction,Biometrics.FaceMatching,Biometrics.FaceSegmentsDetection";
//                String path="C:\\Users\\admin\\Downloads\\Neurotec_Biometric_10_0_SDK_Trial\\Bin\\Win64_x64";
                
                String path=ConfigUtil.getProperty("jna.library.path", "C:\\Users\\admin\\Downloads\\Neurotec_Biometric_10_0_SDK_Trial\\Bin\\Win64_x64");
                System.out.println("Path===>"+path);
                System.setProperty("jna.library.path", path);
                LibraryManager.initLibraryPath();
                if (!NLicense.obtainComponents("/local", 5000, components)) {
                    System.out.println("**************Failed to load Native Libraries***************");
                    System.err.format("Could not obtain licenses for components: %s%n", components);
                    System.exit(-1);
                }
                System.out.println("#############Licensce & Nuetro Native Libraries are loaded successfully...###########");
                isLoaded = true;
            } catch (Exception e) {
                System.out.println("===================>FAiled to Load Native Libraries ==================");
                e.printStackTrace();
            }
        }
    }
}
