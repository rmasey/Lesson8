import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.*;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.event.Event;
import javafx.util.converter.*;

import java.util.Scanner;
import java.util.ArrayList;

import java.util.Optional;
import javafx.collections.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class Main
{
    /* Our app needs a connection to the database, and this is all handled by the DatabaseConnection 
     * class. A public static variable, called database, points to the DatabaseConnection object. */
    public static DatabaseConnection database;
    public static TableView<Book> table;
    public static ObservableList<Book> data = FXCollections.observableArrayList();
    public static  TextField txtFieldBookTitle;
    public static  TextField txtFieldAuthorID;
    private static Book selectedBook = null;

    public static void main(String args[])
    { 
        database = new DatabaseConnection("Inventory.db");        // Initiate the database connection.
        launchFX();              
    }

    private static void launchFX()
    {
        // Initialises JavaFX
        new JFXPanel();
        // Runs initialisation on the JavaFX thread
        Platform.runLater(() -> initialiseGUI());
    }

    private static void initialiseGUI() 
    {
        table = new TableView<Book>();
        table.setEditable(true);
        table.setLayoutX(10);
        table.setLayoutY(10);
        loadAllBooks();
        table.setOnMouseClicked((MouseEvent me) -> {
                selectedBook = table.getSelectionModel().getSelectedItem();
            });

        TableColumn <Book,Integer>colBookID = new TableColumn("Book ID");
        colBookID.setMinWidth(300);
        colBookID.setCellValueFactory(new PropertyValueFactory<Book, Integer>("BookID"));   //relies on a getter method getBookID

        TableColumn <Book, String>colTitle = new TableColumn("Title");
        colTitle.setMinWidth(300);
        colTitle.setCellValueFactory(new PropertyValueFactory<Book, String>("Title"));
        colTitle.setCellFactory(TextFieldTableCell.forTableColumn());
        colTitle.setOnEditCommit((e) -> colTitle_OnEditCommit(e));

        TableColumn <Book, Integer>colAuthorID = new TableColumn("Author ID");
        colAuthorID.setMinWidth(300);
        colAuthorID.setCellValueFactory(new PropertyValueFactory<Book, Integer>("AuthorID"));
        colAuthorID.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        table.getColumns().addAll(colBookID, colTitle, colAuthorID);
        txtFieldBookTitle = new TextField();
        txtFieldBookTitle.setLayoutX(200);
        txtFieldBookTitle.setLayoutY(600);
        txtFieldBookTitle.setPrefWidth(300);
        txtFieldBookTitle.setPromptText("Enter book title");

        txtFieldAuthorID = new TextField();
        txtFieldAuthorID.setLayoutX(550);
        txtFieldAuthorID.setLayoutY(600);
        txtFieldAuthorID.setPromptText("Enter author ID");

        Button btnAdd = new Button("Add");
        btnAdd.setLayoutX(50);
        btnAdd.setLayoutY(600);
        btnAdd.setOnAction((ActionEvent ae) -> addBook());

        Button btnDelete = new Button("Delete Selected");
        btnDelete.setLayoutX(50);
        btnDelete.setLayoutY(500);
        btnDelete.setOnAction((ActionEvent ae) -> deleteBook());

        Stage stage = new Stage();
        stage.setTitle("Inventory Application");
        stage.setResizable(false);
        Pane rootPane = new Pane();
        stage.setScene(new Scene(rootPane));                        
        stage.setWidth(1024);
        stage.setHeight(768);
        stage.setOnCloseRequest((WindowEvent we) -> terminate(we));
        stage.show(); 

        rootPane.getChildren().addAll(table);
        rootPane.getChildren().addAll(txtFieldBookTitle,txtFieldAuthorID, btnAdd, btnDelete);
    }

    public static void loadAllBooks()
    {
        data.clear();
        BookService.readAll(data);
        table.setItems(data);
    }

    public static void addBook()
    {
        Book newBook = new Book(0, txtFieldBookTitle.getText(),Integer.parseInt(txtFieldAuthorID.getText()));
        BookService.save(newBook);
        loadAllBooks();
    }

    public static void deleteBook()
    {
        if (selectedBook == null) {
            return;
        }
        BookService.deleteById(selectedBook.getBookID());
        loadAllBooks();
    }

    public static void colTitle_OnEditCommit(Event e)
    {
        System.out.println("Updating database...");

        TableColumn.CellEditEvent<Book,String>ce;
        ce = (TableColumn.CellEditEvent<Book, String>) e;

        try 
        {
            PreparedStatement statement = Main.database.newStatement("UPDATE Books set Title = ? WHERE BookID =" + selectedBook.getBookID() +" ");             
            statement.setString(1,ce.getNewValue());

            if (statement != null) {
                Main.database.executeUpdate(statement);
            }
        }
        catch (SQLException resultsexception) {
            System.out.println("Database result processing error: " + resultsexception.getMessage());
        }
        loadAllBooks();

    }
    // 
    //     public static void colAuthorID_OnEditCommit()
    //     {
    //         TableColumn.CellEditEvent<Book,Integer>ce;
    //         ce = (TableColumn.CellEditEvent<Book, Integer>) e;
    //         Book m = ce.getRowValue();
    //         m.setAuthorID(ce.getNewValue());
    //     }

    public static void terminate(WindowEvent we)
    {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Look, a Confirmation Dialog");
        alert.setContentText("Are you sure you want to quit?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            // user chose OK so close the application
            System.out.println("Closing database connection and terminating application...");     
            if (database != null){ 
                database.disconnect(); 
            }
            System.exit(0);   
        } else {
            // user chose CANCEL or closed the dialog so do nothing
            we.consume();
        }
    }
}
