package com.example.yhwasongtest.place.controller;

import com.example.yhwasongtest.common.FileSecurity;
import com.example.yhwasongtest.place.dto.PlaceDto;
import com.example.yhwasongtest.place.model.PictureModel;
import com.example.yhwasongtest.place.model.PlaceModel;
import com.example.yhwasongtest.place.model.ReviewModel;
import com.example.yhwasongtest.place.repository.PictureRepository;
import com.example.yhwasongtest.place.service.PlaceService;
import com.example.yhwasongtest.youtube.model.YoutubeModel;
import com.mysql.cj.xdevapi.JsonArray;
import com.mysql.cj.xdevapi.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping(value = "/api/v1")
public class PlaceController {

    private static final Logger logger = LoggerFactory.getLogger(PlaceController.class);

    private final PlaceService placeService;
    private PictureRepository pictureRepository;

    @Autowired
    public PlaceController(PlaceService placeService,
                           PictureRepository pictureRepository) {
        this.placeService = placeService;
        this.pictureRepository = pictureRepository;
    }

    @GetMapping(value = "/place")
    public ResponseEntity getPlace(){

        try {
            List<PlaceModel> placeModels = placeService.getPlace();

            JSONArray jsonArray = new JSONArray();
            for (PlaceModel placeModel : placeModels) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", placeModel.getName());
                jsonObject.put("area", placeModel.getArea());
                jsonObject.put("url", placeModel.getUrl());
                jsonObject.put("number", placeModel.getNumber());
                jsonObject.put("subCategory", placeModel.getSubCategory());
                jsonObject.put("recommend", placeModel.getRecommend());
                jsonObject.put("view", placeModel.getView());
                jsonObject.put("fileId", placeModel.getFileId());

                String str= ""; // place 에 간 유투브들
                List<YoutubeModel> youtubes = new ArrayList<>();
                youtubes = placeModel.getYoutubes();
                for(YoutubeModel youtube : youtubes){
                    str += "#"+youtube.getChannelTitle();
                }
                jsonObject.put("youtube", str);
                jsonArray.put(jsonObject);
            }

            return  new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @GetMapping(value = "/place/{category}")
    public List<PlaceModel> getPlaceByCategory(@PathVariable String category){
        List<PlaceModel> placeModels = placeService.getPlaceListBySubCategory(category);
        return placeModels;
    }

    @PostMapping(value = "/place/{name}")
    public ResponseEntity getPlaceByName(@PathVariable String name) {
        PlaceModel placeModel = placeService.getPlaceByName(name);
        return new ResponseEntity<>(placeModel, HttpStatus.OK);
    }

    @PostMapping(value = "/place")
    public ResponseEntity putPlace(@RequestParam("file") MultipartFile file, @RequestBody PlaceDto model) {

        String result = "";
        PlaceModel placeModel = new PlaceModel();
        try {
            if(!file.isEmpty()){
                model.setFileId(placeService.saveFile(file));
            }

            placeModel.setName(model.getName()); // 장소 이름
            placeModel.setArea(model.getArea()); // 지역
            placeModel.setNumber(model.getNumber());
            placeModel.setUrl(model.getUrl());
            placeModel.setSubCategory(model.getSubCategory());

            placeModel = placeService.putPlace(placeModel);

            return new ResponseEntity<>(placeModel, HttpStatus.OK);

        } catch (Exception error) {
            System.err.println("putPlace Error ==> " + error);
            return new ResponseEntity<>(placeModel, HttpStatus.BAD_REQUEST);

        }
    }

    @DeleteMapping(value = "/place/{name}")
    public ResponseEntity deletePlace(@PathVariable(name = "name",required = true) String name) {

        placeService.deletePlace(name);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping(value = "/location")
    public ResponseEntity getLocation(){
        String result = placeService.getLocation();
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void addView(@CookieValue(name = "login", defaultValue = "null") String cookie,
                             @RequestParam(name = "id") long id,
                             @RequestParam(name = "placeName") String placeName,
                             HttpServletResponse response){
        // 쿠키에 아이디가 없다면 추가해주기.
        if(!cookie.contains("loginCookie" + placeName)){
            cookie += id + "/";
            placeService.addView(placeName);
        }
        response.addCookie(new Cookie("loginCookie" +placeName, cookie));

    }

    @GetMapping(value = "/classification")
    public void setClassification() {
        placeService.getFoodCategory();
        placeService.getDessertCategory();
    }

    public ResponseEntity getFile(String placeName) {
        try{
        PlaceModel placeModel = placeService.getPlaceByName(placeName);
        if (placeModel.getFileId() == null)
            return null;

        PictureModel pictureModel = pictureRepository.findById(placeModel.getFileId()).orElseThrow(IllegalArgumentException::new);;
        String filename = pictureModel.getFileName();
        String encodeFileName = URLEncoder.encode(filename, "UTF-8");
        Resource file = placeService.loadFile(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodeFileName + "\"")
                .body(file);

        }catch (Exception e){
            logger.info("PlaceController.js error =>" , e.toString());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
