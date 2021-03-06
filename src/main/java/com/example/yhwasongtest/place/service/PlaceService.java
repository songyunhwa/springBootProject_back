package com.example.yhwasongtest.place.service;

import com.example.yhwasongtest.common.FileSecurity;
import com.example.yhwasongtest.place.dto.PlaceDto;
import com.example.yhwasongtest.place.model.*;
import com.example.yhwasongtest.place.repository.*;
import com.example.yhwasongtest.youtube.dto.YoutubeDto;
import com.example.yhwasongtest.youtube.model.YoutubeModel;
import com.example.yhwasongtest.youtube.repository.YoutubeRepository;
import com.example.yhwasongtest.youtube.service.YoutubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class PlaceService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceService.class);

    private PlaceRepository placeRepository;
    private LocationService locationService;
    private FoodRepository foodRepository;
    private DessertRepository dessertRepository;
    private YoutubeService youtubeService;

    @Autowired
    public PlaceService(PlaceRepository placeRepository,
                        LocationService locationService,
                        FoodRepository foodRepository,
                        DessertRepository dessertRepository,
                        YoutubeService youtubeService) {
        this.placeRepository = placeRepository;
        this.locationService = locationService;
        this.foodRepository = foodRepository;
        this.dessertRepository = dessertRepository;
        this.youtubeService = youtubeService;
    }

    public List<PlaceModel> getPlaces() {
        List<PlaceModel> placeModelList = this.placeRepository.findByViewAndSubCategory();
        for(PlaceModel placeModel : placeModelList) {
            // ???????????? ????????? ???????????????
            // ?????? ?????? ???????????? ????????? ????????? ??????.
            List<YoutubeModel> youtubeModels = youtubeService.getYoutubeModelByPlaceId(placeModel.getId());
            if (youtubeModels.size() != placeModel.getYoutubes().size()) {
                placeModel.setYoutubes(youtubeModels);
            }
        }
        return placeModelList;
    }

    public PlaceModel getPlace(long id) {
        PlaceModel placeModel = this.placeRepository.findById(id);
        return placeModel;
    }

    public PlaceModel getPlaceByName(String name) {
        return this.placeRepository.findByName(name);
    }

    public List<PlaceModel> getPlaceListBySubCategory(String category) {
        List<PlaceModel> placeModelList;
        if (category.equals("all")) {
            placeModelList = this.placeRepository.findByViewAndSubCategory();
        } else {
            placeModelList = this.placeRepository.findBySubCategoryOrderByViewDesc(category);
        }
        return placeModelList;
    }

    public PlaceModel putPlace(PlaceDto placeDto) throws Exception {
        if (placeDto.getSubCategory() == null) {
            throw new Exception("sub_category ??? ????????????.");
        }
        if (placeDto.getName() == null) {
            throw new Exception("name ??? ????????????.");
        }

        // ????????? ????????? ?????? x
        String pattern = "/^[a-zA-Z]*$/";
        boolean isMatch = Pattern.matches(pattern, placeDto.getName());
        if (isMatch) throw new Exception("?????? ????????? ????????? ????????????.");

        PlaceModel existPlace = placeRepository.findByNameAndSubCategory(placeDto.getName(), placeDto.getSubCategory());
        if (existPlace == null) {
            existPlace = new PlaceModel();
            existPlace.setName(placeDto.getName().replace(" ", ""));
            existPlace.setView(0);
            existPlace.setRecommend(0);
            existPlace.setSubCategory(placeDto.getSubCategory());
            List<YoutubeModel> youtubes = new ArrayList<YoutubeModel>();
            existPlace.setYoutubes(youtubes);
            existPlace = placeRepository.save(existPlace);
        }

        List<YoutubeModel> youtubeModels = new ArrayList<>();
        // ????????? ????????? ????????? ?????? ??????
        if (placeDto.getYoutubes() != null) {
            for (YoutubeDto youtube : placeDto.getYoutubes()) {
                YoutubeModel youtubeModel = youtubeService.getYoutubeModel(youtube.getVideoId());
                if(youtubeModel == null) {
                    youtubeModel = new YoutubeModel();
                    youtubeModel.setTitle(youtube.getTitle());
                    youtubeModel.setChannelTitle(youtube.getChannelTitle());
                    youtubeModel.setVideoId(youtube.getVideoId());
                    youtubeModel.setPlace(existPlace);
                }
                youtubeModels.add(youtubeModel);
            }
        }
        // ????????? ???????????? ?????? ????????? ??????
        for (YoutubeModel youtubeModel : existPlace.getYoutubes()) {
            if(!youtubeModels.contains(youtubeModel)){
                youtubeService.deleteYoutube(youtubeModel.getVideoId());
                existPlace.getYoutubes().remove(youtubeModel);
            }
        }
        existPlace.getYoutubes().clear();
        existPlace.setYoutubes(youtubeModels);
        existPlace = placeRepository.save(existPlace);

        // ?????? ??????
        locationService.saveLocation(existPlace, placeDto);

        return existPlace;
    }

    public void deletePlace(String name) {
        PlaceModel placeModel = placeRepository.findByName(name);
        placeRepository.delete(placeModel);
    }

    // place ?????????????????? ????????????
    public void getFoodCategory() {

        List<FoodModel> foodModelList = foodRepository.findAll();
        List<PlaceModel> placeModelList;
        for (FoodModel foodModel : foodModelList) {
            placeModelList = placeRepository.findByNameContaining(foodModel.getIncluded());
            for (PlaceModel placeModel : placeModelList) {
                if (!placeModel.getSubCategory().equals(foodModel.getSubCategory()))
                    placeModel.setSubCategory(foodModel.getSubCategory());
            }
            placeRepository.saveAll(placeModelList);
        }

        placeModelList = placeRepository.findBySubCategory("food");
        placeRepository.deleteAll(placeModelList);
    }

    public void getDessertCategory() {

        List<DessertModel> dessertModelList = dessertRepository.findBySubCategory("etc");
        List<PlaceModel> placeModelList;
        for (DessertModel dessertModel : dessertModelList) {
            placeModelList = placeRepository.findByNameContaining(dessertModel.getIncluded());
            for (PlaceModel placeModel : placeModelList) {
                if (!placeModel.getSubCategory().equals(dessertModel.getSubCategory()))
                    placeModel.setSubCategory(dessertModel.getSubCategory());
            }
            placeRepository.saveAll(placeModelList);
        }
    }


    // ?????? ?????? ??????
    public void deletePlaceContaingEng() {
        List<PlaceModel> places = placeRepository.findByNameContaingEng();
        for (PlaceModel place : places) {
            placeRepository.delete(place);
        }
    }

    // ????????? ???????????? ??????
    public void deletePlaceYoutube() {
        List<PlaceModel> places = placeRepository.findBySubCategory("etc");
        for (PlaceModel place : places) {
            List<YoutubeModel> youtubeModels = place.getYoutubes();

            if (youtubeModels != null && youtubeModels.size() > 1) {
                HashSet<YoutubeModel> set = new HashSet<YoutubeModel>();
                for (int t = 0; t < youtubeModels.size(); t++) {
                    set.add(youtubeModels.get(t));
                }
                youtubeModels.clear();
                Iterator iter = set.iterator();    // Iterator ??????
                while (iter.hasNext()) {//?????? ????????? true ????????? false
                    youtubeModels.add((YoutubeModel) iter.next());
                }
                placeRepository.save(place);
            }
        }
    }

    public List<PlaceModel> searchPlace(String msg) {

        List<PlaceModel> title_places = placeRepository.findByChannelTitle(msg);
        List<PlaceModel> name_places = placeRepository.findByNameContaing(msg);

        List<PlaceModel> places = new ArrayList<PlaceModel>();
        name_places.forEach(placeModel -> {
            if (!places.contains(placeModel))
                places.add(placeModel);
        });
        title_places.forEach(placeModel -> {
            if (!places.contains(placeModel))
                places.add(placeModel);
        });


        return places;
    }

    public List<DessertModel> getCategory() {
        return dessertRepository.findAll();
    }
}
