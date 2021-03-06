package it.unisa.compressionedati.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

public class UtilCompression {


    public static String compressTextAndReturnB64(String text) throws IOException {
        return new String(Base64.getEncoder().encode(compressText(text)));
    }

    public static String decompressTextB64(String b64Compressed) throws IOException {
        byte[] decompressedBArray = decompressText(Base64.getDecoder().decode(b64Compressed));
        return new String(decompressedBArray, StandardCharsets.UTF_8);
    }

    public static byte[] compressText(String text) throws IOException {
        return compressText(text.getBytes());
    }

    public static byte[] compressText(byte[] bArray) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(os)) {
            dos.write(bArray);
        }
        return os.toByteArray();
    }

    public static byte[] decompressText(byte[] compressedTxt) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStream ios = new InflaterOutputStream(os)) {
            ios.write(compressedTxt);
        }

        return os.toByteArray();
    }


    public static byte[] compressImageInJpeg(byte[] im, float quality) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(im);
        BufferedImage image = ImageIO.read(bais);
        // The important part: Create in-memory stream
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)) {

            // NOTE: The rest of the code is just a cleaned up version of your code
            // Obtain writer for JPEG format
            ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("JPEG").next();

            // Configure JPEG compression: 70% quality
            ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(quality);

            // Set your in-memory stream as the output
            jpgWriter.setOutput(outputStream);

            // Write image as JPEG w/configured settings to the in-memory stream
            // (the IIOImage is just an aggregator object, allowing you to associate
            // thumbnails and metadata to the image, it "does" nothing)
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);

            // Dispose the writer to free resources
            jpgWriter.dispose();
        }

// Get data for further processing...
        byte[] jpegData = compressed.toByteArray();
        return jpegData;
    }



    public static String write_content(String input, String name_file){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            String myHash = DatatypeConverter.printHexBinary(digest).toUpperCase();

            JSONParser jsonParser = new JSONParser();
            File fr=new File(name_file);
            JSONArray jsonArray=null;
            if(fr.exists()){
                jsonArray = (JSONArray)jsonParser.parse(new FileReader(name_file));
                //jsonArray = (JSONArray)obj;

            }
            if(jsonArray==null)
                jsonArray=new JSONArray();

            JSONObject jsObj = new JSONObject();
            jsObj.put(myHash,input);
            jsonArray.add(jsObj);

            FileWriter file = new FileWriter(name_file);
            file.write(jsonArray.toJSONString());
            file.flush();
            file.close();

            return myHash;
        } catch (NoSuchAlgorithmException | FileNotFoundException ex) {
            Logger.getLogger(UtilCompression.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> read_content(String name_file) throws IOException, ParseException {
        //JSON parser object to parse read file
        List<String> keys=new ArrayList();

        String content_file = new String ( Files.readAllBytes( Paths.get(name_file) ) );
        //String to_p = decrypterText(content_file);

        JSONParser jsonParser = new JSONParser();
        JSONArray  jsarr = (JSONArray) jsonParser.parse(content_file);
        String parse="";
        Iterator it = jsarr.iterator();

        while(it.hasNext()){
            JSONObject obj = (JSONObject) it.next();
            obj.keySet().forEach(keyStr ->
            {
                Object keyvalue = obj.get(keyStr);
                keys.add(keyStr.toString());
            });
        }
        //JSONObject jsObj = (JSONObject) jsonParser.parse(to_p);
        //String parse = jsObj.get(key)!=null ? jsObj.get(key).toString() : null;

        return keys;

    }

    public static String find_content_from_key_json(String name_file, String key) throws IOException, ParseException {
        //JSON parser object to parse read file
        String content=null;
        List<String> list=new ArrayList<>();
        String content_file = new String ( Files.readAllBytes( Paths.get(name_file) ) );
        //String to_p = decrypterText(content_file);

        JSONParser jsonParser = new JSONParser();
        JSONArray  jsarr = (JSONArray) jsonParser.parse(content_file);
        String parse="";
        Iterator it = jsarr.iterator();

        while(it.hasNext()){


                JSONObject obj = (JSONObject) it.next();
                obj.keySet().forEach(keyStr ->
                {
                    Object keyvalue = obj.get(keyStr);
                    if(keyStr.equals(key))
                        list.add(keyvalue.toString());
                });

        }
        //JSONObject jsObj = (JSONObject) jsonParser.parse(to_p);
        //String parse = jsObj.get(key)!=null ? jsObj.get(key).toString() : null;

        return list!=null && list.size()>0 ? list.get(0) : null;

    }

    public static String encrypterText(String value) {
        try {
            SecretKeySpec key = new SecretKeySpec("My_Secret_Key".getBytes(), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainTxtBytes = value.getBytes("UTF-8");
            byte[] encBytes = cipher.doFinal(plainTxtBytes);
            return new sun.misc.BASE64Encoder().encode(encBytes);
        } catch (Exception ex) {
            return value;
        }
    }

    public static String decrypterText(String value) {
        try {
            SecretKeySpec key = new SecretKeySpec("My_Secret_Key".getBytes(), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encBytes = new sun.misc.BASE64Decoder().decodeBuffer(value);
            byte[] plainTxtBytes = cipher.doFinal(encBytes);
            return new String(plainTxtBytes);
        } catch (Exception ex) {
            return value;
        }
    }

}
