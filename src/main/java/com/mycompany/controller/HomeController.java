package com.mycompany.controller;// HomeController.java
import com.mycompany.utils.ApiClient;
import com.mycompany.utils.SessionManager;
import com.google.gson.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView; // hoặc TableView tùy FXML

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private ListView<String> auctionListView; // thêm vào Home.fxml

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadAuctions();
    }

    private void loadAuctions() {
        String token = SessionManager.getInstance().getCurrentToken();
        String json = ApiClient.getAuctions(token);

        if (json == null) return;

        JsonArray array = new Gson().fromJson(json, JsonArray.class);
        javafx.collections.ObservableList<String> items =
                FXCollections.observableArrayList();

        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            String tenPhien = obj.get("tenPhien").getAsString();
            double gia = obj.get("giaHienTai").getAsDouble();
            String trangThai = obj.get("trangThai").getAsString();
            items.add(tenPhien + " | " + String.format("%,.0f VNĐ", gia)
                    + " | " + trangThai);
        }
        auctionListView.setItems(items);
    }

}