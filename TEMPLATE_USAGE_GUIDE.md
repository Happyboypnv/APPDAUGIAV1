# Template System Usage Guide (Auto-Update Version)

## Overview

This guide explains the **new auto-update system** using `<fx:include>` that ensures all pages automatically reflect navbar changes.

### Component Files

1. **NavbarComponent.fxml** - The reusable navbar sidebar (containing the 3 menu items)
2. **Template.fxml** - Example template page (reference page)
3. **Home.fxml** - Home page (includes NavbarComponent)
4. **Profile.fxml** - Profile page (includes NavbarComponent)

### How It Works

- **NavbarComponent.fxml** is the single source of truth for the navbar
- All pages `<fx:include>` this component
- When you modify NavbarComponent.fxml, **all pages automatically update** ✅

## Architecture

```
NavbarComponent.fxml (Single Source of Truth)
    ↓
Home.fxml ────┐ (includes NavbarComponent)
              ├─→ All pages have identical navbar
Profile.fxml ─┤ (includes NavbarComponent)
              │
Future Pages ─┘ (includes NavbarComponent)
```

## Quick Comparison: Old vs New

### OLD (Copy-Paste) ❌
- Each page had its own VBox navbar code
- Changes required updating every file manually
- Risk of inconsistency

### NEW (fx:include) ✅
- Navbar is defined once in NavbarComponent.fxml
- All pages include it automatically
- Single change updates everywhere

## Creating New Pages

### Step 1: Create FXML File
Create a new FXML file in `src/main/resources/view/`
Example: `FinanceManagement.fxml`

### Step 2: Use This Template Structure

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
           style="-fx-background-color: #1c254b;" 
           styleClass="my-custom-node" 
           stylesheets="@../style/nen.css" 
           xmlns="http://javafx.com/javafx/25" 
           xmlns:fx="http://javafx.com/fxml/1" 
           fx:controller="com.mycompany.Controller.YourPageController">
   <children>
      <!-- Avatar Image (Top Right) -->
      <ImageView fx:id="avatarImage" fitHeight="40" fitWidth="40" 
                 onMouseClicked="#handleAvatarClick" pickOnBounds="true" 
                 preserveRatio="true" StackPane.alignment="TOP_RIGHT">
         <StackPane.margin>
            <Insets right="30.0" top="20.0" />
         </StackPane.margin>
         <cursor>
            <Cursor fx:constant="HAND" />
         </cursor>
         <image>
            <Image url="@../image/default_avatar.jpg" />
         </image>
      </ImageView>
      
      <!-- ⭐ Auto-Updating Navbar Component ⭐ -->
      <fx:include source="NavbarComponent.fxml" StackPane.alignment="TOP_LEFT" />
      
      <!-- Your Page Content Here -->
      <Label text="Your Page Title" textFill="WHITE">
         <font>
            <Font size="31.0" />
         </font>
      </Label>
      
      <!-- Home Icon (Top Left) -->
      <ImageView fx:id="homeIcon" fitHeight="40.0" fitWidth="40.0" 
                 onMouseClicked="#returnToHome" pickOnBounds="true" 
                 preserveRatio="true" StackPane.alignment="TOP_LEFT">
         <cursor>
            <Cursor fx:constant="HAND" />
         </cursor>
         <StackPane.margin>
            <Insets left="30.0" top="20.0" />
         </StackPane.margin>
         <image>
            <Image url="@../image/home.png" />
         </image>
      </ImageView>
   </children>
</StackPane>
```

**Key Points:**
- Line: `<fx:include source="NavbarComponent.fxml" StackPane.alignment="TOP_LEFT" />`
- This includes the navbar from NavbarComponent.fxml
- Changes to NavbarComponent.fxml will automatically appear on this page

### Step 3: Create Controller

```java
package com.mycompany.Controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class YourPageController extends NavBarController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        // Your page-specific logic here
    }
}
```

## Modifying the Navbar

To change navbar items, colors, or styling:

1. Open `src/main/resources/view/NavbarComponent.fxml`
2. Make your changes
3. **All pages automatically reflect changes** ✅

### Example: Change Navbar Item Text

Edit `NavbarComponent.fxml`:
```xml
<Label fx:id="createAuction" ... text="Tạo phiên đấu giá" />
```
↓ Change to:
```xml
<Label fx:id="createAuction" ... text="New Text Here" />
```
↓ **All pages that include NavbarComponent now show the new text!**

### Example: Change Navbar Colors

Edit `NavbarComponent.fxml`:
```xml
<VBox ... style="-fx-background-color: #181c2f; ..." />
```
↓ Change to:
```xml
<VBox ... style="-fx-background-color: #newColor; ..." />
```
↓ **All pages instantly have the new color!**

## File Reference

| File | Purpose | Location |
|------|---------|----------|
| **NavbarComponent.fxml** | Reusable navbar (single source of truth) | `src/main/resources/view/` |
| **Template.fxml** | Example/reference page | `src/main/resources/view/` |
| **Home.fxml** | Home page (includes navbar) | `src/main/resources/view/` |
| **Profile.fxml** | Profile page (includes navbar) | `src/main/resources/view/` |
| **NavBarController.java** | Base controller for navbar functionality | `src/main/java/com/mycompany/Controller/` |
| **HomeController.java** | Home page specific logic | `src/main/java/com/mycompany/Controller/` |
| **ProfileController.java** | Profile page specific logic | `src/main/java/com/mycompany/Controller/` |

## Navbar Components (fx:id values)

These IDs are available in NavbarComponent.fxml:

```xml
fx:id="navBar"                  <!-- The VBox container itself -->
fx:id="createAuction"           <!-- Tạo phiên đấu giá -->
fx:id="transactionHistory"      <!-- Lịch sử giao dịch -->
fx:id="financeManagement"       <!-- Tài chính -->
```

## Adding Click Handlers to Navbar Items

If you want navbar items to navigate to other pages:

### 1. Add `onMouseClicked` to NavbarComponent.fxml

```xml
<Label fx:id="createAuction" ... onMouseClicked="#handleCreateAuctionClick" />
```

### 2. Add Handler Method to Your Controller

```java
@FXML
public void handleCreateAuctionClick(MouseEvent event) {
    try {
        HandleNavigationAndAlert.getInstance().handleGoToCreateAuction(event);
    } catch (IOException e) {
        showAlert(Alert.AlertType.ERROR, "Lỗi", "Navigation failed");
    }
}
```

### 3. Add Navigation Method to HandleNavigationAndAlert.java

```java
public void handleGoToCreateAuction(Event event) throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource("/view/CreateAuction.fxml"));
    Scene scene = new Scene(root);
    Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
    window.setScene(scene);
    window.show();
}
```

## Controller Hierarchy

```
implements Initializable
        ↓
NavBarController (handles navbar: avatar menu, home icon, etc.)
        ↓
HomeController (Home.fxml specific logic)
ProfileController (Profile.fxml specific logic)
YourPageController (YourPage.fxml specific logic)
```

## Styles and Customization

### Navbar Styling (in NavbarComponent.fxml)

```xml
<VBox ... style="-fx-background-color: #181c2f;          <!-- Background color -->
                 -fx-padding: 80 0 0 20;                 <!-- Padding -->
                 -fx-background-radius: 15;" />          <!-- Border radius -->
```

### Navbar Item Styling

```xml
<Label ... style="-fx-text-fill: #b6bfff;               <!-- Text color -->
                  -fx-font-size: 18px;                  <!-- Font size -->
                  -fx-font-family: 'Montserrat';        <!-- Font family -->
                  -fx-padding: 10 0 10 10;" />           <!-- Padding -->
```

### Page Background (in each page FXML)

```xml
<StackPane ... style="-fx-background-color: #1c254b;" />
```

## Color References

- Navbar background: `#181c2f`
- Navbar text: `#b6bfff`
- Page background: `#1c254b`
- Primary accent: `#EE7455`

## Benefits of This System

✅ **Single Source of Truth** - Navbar defined once in NavbarComponent.fxml
✅ **Auto-Update** - Change NavbarComponent → All pages update automatically
✅ **DRY (Don't Repeat Yourself)** - No code duplication
✅ **Maintainability** - Single place to update navbar
✅ **Consistency** - All pages have identical navbar
✅ **Scalability** - Easy to add new pages following the same pattern

## Common Tasks

### Add a New Navbar Item

1. Edit `NavbarComponent.fxml`
2. Add new `<Label>` in the VBox:
```xml
<Label fx:id="newItem" style="..." text="New Item Name">
   <VBox.margin><Insets /></VBox.margin>
   <cursor><Cursor fx:constant="HAND" /></cursor>
</Label>
```
3. All pages automatically have the new item ✅

### Change Navbar Color

1. Edit `NavbarComponent.fxml`
2. Modify the style property
3. All pages instantly update ✅

### Change Navbar Width

1. Edit `NavbarComponent.fxml`
2. Modify `maxWidth`, `minWidth`, `prefWidth`
3. All pages instantly update ✅

## Troubleshooting

### Navbar not showing on my new page?
- Make sure you have `<fx:include source="NavbarComponent.fxml" StackPane.alignment="TOP_LEFT" />`
- Ensure the path `NavbarComponent.fxml` is correct

### Controller methods not working?
- Make sure your controller extends `NavBarController`
- Ensure method visibility is `@FXML public`

### fx:id references not updating?
- NetBeans/IDEs cache FXML. Try:
  - Clean and rebuild project
  - Restart IDE
  - Clear cache

## Next Steps

1. Update existing pages to use `<fx:include>` (Home ✓, Profile ✓)
2. Create new pages using the template structure
3. Add click handlers to navbar items as needed
4. Customize navbar styling in NavbarComponent.fxml

