# ⚡ Quick Reference: Auto-Update System

## What Changed?
✅ Created centralized navbar component that automatically updates all pages

## Key Files

| File | Purpose |
|------|---------|
| `NavbarComponent.fxml` | 🔑 **Single source of truth** for navbar |
| `Home.fxml` | Includes NavbarComponent |
| `Profile.fxml` | Includes NavbarComponent |
| `Template.fxml` | Reference page, includes NavbarComponent |

## The Magic Line
All pages now have this line that pulls navbar from a single source:
```xml
<fx:include source="NavbarComponent.fxml" StackPane.alignment="TOP_LEFT" />
```

## How to Use

### To Change Navbar: Edit One File
```
NavbarComponent.fxml
    ↓
All pages automatically update! ✨
```

### To Create New Page

**1. Copy this FXML template:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.Cursor?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.image.Image?>
<?import javafx.scene.image.ImageView?>
<?import javafx.scene.layout.StackPane?>
<?import javafx.scene.text.Font?>

<StackPane maxHeight="-Infinity" maxWidth="-Infinity" minHeight="-Infinity" minWidth="-Infinity" 
           prefHeight="710.0" prefWidth="893.0" 
           style="-fx-background-color: #1c254b;" styleClass="my-custom-node" 
           stylesheets="@../style/nen.css" 
           xmlns="http://javafx.com/javafx/25" xmlns:fx="http://javafx.com/fxml/1" 
           fx:controller="com.mycompany.Controller.YourPageController">
   <children>
      <ImageView fx:id="avatarImage" fitHeight="40" fitWidth="40" onMouseClicked="#handleAvatarClick" 
                 pickOnBounds="true" preserveRatio="true" StackPane.alignment="TOP_RIGHT">
         <StackPane.margin><Insets right="30.0" top="20.0" /></StackPane.margin>
         <cursor><Cursor fx:constant="HAND" /></cursor>
         <image><Image url="@../image/default_avatar.jpg" /></image>
      </ImageView>
      
      <!-- ⭐ THIS IS THE KEY LINE - Includes navbar automatically -->
      <fx:include source="NavbarComponent.fxml" StackPane.alignment="TOP_LEFT" />
      
      <!-- YOUR PAGE CONTENT -->
      <Label text="Your Page Title" textFill="WHITE">
         <font><Font size="31.0" /></font>
      </Label>
      
      <ImageView fx:id="homeIcon" fitHeight="40.0" fitWidth="40.0" onMouseClicked="#returnToHome" 
                 pickOnBounds="true" preserveRatio="true" StackPane.alignment="TOP_LEFT">
         <cursor><Cursor fx:constant="HAND" /></cursor>
         <StackPane.margin><Insets left="30.0" top="20.0" /></StackPane.margin>
         <image><Image url="@../image/home.png" /></image>
      </ImageView>
   </children>
</StackPane>
```

**2. Create a controller:**
```java
package com.mycompany.Controller;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class YourPageController extends NavBarController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
    }
}
```

## What Gets Included?

When you use `<fx:include source="NavbarComponent.fxml" />`, you automatically get:
- ✅ Tạo phiên đấu giá
- ✅ Lịch sử giao dịch
- ✅ Tài chính

All with consistent styling and behavior across all pages.

## Modify Navbar

### Change text:
```xml
<!-- In NavbarComponent.fxml -->
<Label fx:id="createAuction" text="New Text" />
```
→ All pages instantly show "New Text"

### Change color:
```xml
<!-- In NavbarComponent.fxml -->
<VBox style="-fx-background-color: #1c254b;" />
```
→ All pages instantly have new background

### Add new item:
```xml
<!-- In NavbarComponent.fxml inside <VBox> -->
<Label fx:id="newItem" text="New Item Name">
   <VBox.margin><Insets /></VBox.margin>
   <cursor><Cursor fx:constant="HAND" /></cursor>
</Label>
```
→ All pages instantly show new item

## Navbar Item IDs

Use these to add click handlers:
```xml
fx:id="createAuction"       <!-- Create auction -->
fx:id="transactionHistory"  <!-- Transaction history -->
fx:id="financeManagement"   <!-- Finance management -->
```

## Colors Used
```
Background: #1c254b
Navbar: #181c2f
Text: #b6bfff
Accent: #EE7455
```

## Benefits
- Single source of truth for navbar
- Change once, update everywhere
- No code duplication
- Consistent across all pages
- Easy to scale

## Full Documentation
See:
- `TEMPLATE_USAGE_GUIDE.md` - Complete guide
- `AUTO_UPDATE_REFACTORING_SUMMARY.md` - What changed

