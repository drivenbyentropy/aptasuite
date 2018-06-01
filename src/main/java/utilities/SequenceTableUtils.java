package utilities;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptamer.datastructures.Experiment;

/**
 * @author Roland09, hoinkaj
 *
 * The basic idea for this class was authored by GitHub user 
 * Roland09, see https://gist.github.com/Roland09/4a9bbec634ca4a081b66
 * I adopted it for my needs and added a context menu
 * 
 */
public class SequenceTableUtils {
	
	private TableView<?> table;
	private int sequence_column_id;
	private RadioButton showPrimersRadioButton = null;
	private ContextMenu contextMenu;
	private Experiment experiment = Configuration.getExperiment();
	private String newline = System.lineSeparator();
	
	
	public SequenceTableUtils(TableView<?> table, int sequence_column_id, RadioButton showPrimersRadioButton) {
		
		this.table = table;
		this.sequence_column_id = sequence_column_id;
		this.showPrimersRadioButton = showPrimersRadioButton;
		
		installCopyHandler();
		
	}
	
	public SequenceTableUtils(TableView<?> table, int sequence_column_id) {
		
		this(table, sequence_column_id, null);	
		
	}

	/**
	 * Install the keyboard handler:
	 *   + CTRL + C = copy to clipboard
	 * @param table
	 */
	private void installCopyHandler() {

		// install copy/paste keyboard handler
		table.setOnKeyPressed(new TableKeyEventHandler());
		
		contextMenu = createContextMenu();  
		
		table.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
			 
            @Override
            public void handle(ContextMenuEvent event) {
 
                contextMenu.show(table, event.getScreenX(), event.getScreenY());
            	
            }
        });

	}

	
	private ContextMenu createContextMenu() {
		
		// Create ContextMenu
        ContextMenu contextMenu = new ContextMenu();
 
        MenuItem item1 = new MenuItem("Copy Sequence(s) Only");
        item1.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				copySequencesToClipboard();
				
			}
        	
        	
        });
 
        MenuItem item2 = new MenuItem("Copy Entire Row(s)");
        item2.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				copyRowToClipboard();
				
			}
        	
        });
        
        
        MenuItem item3 = new MenuItem("Close Menu");
        item3.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				contextMenu.hide();
				
			}
        	
        });
        
        // Add MenuItem to ContextMenu
        contextMenu.getItems().addAll(item1, item2, item3);
        
        return contextMenu;
		
	}
	
	/**
	 * Copy/Paste keyboard event handler.
	 * The handler uses the keyEvent's source for the clipboard data. The source must be of type TableView.
	 */
	public class TableKeyEventHandler implements EventHandler<KeyEvent> {

		KeyCodeCombination copyKeyCodeCompination = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);

		public void handle(final KeyEvent keyEvent) {

			if (copyKeyCodeCompination.match(keyEvent)) {

				if( keyEvent.getSource() instanceof TableView) {
				
					// copy to clipboard
					copySequencesToClipboard();
	
					System.out.println("Selection copied to clipboard");
	
					// event is handled, consume it
					keyEvent.consume();
					
				}

			}

		}

	}

	/**
	 * Get table selection and copy it to the clipboard.
	 * @param table
	 */
	private void copySequencesToClipboard() {

		StringBuilder clipboardString = new StringBuilder();

		ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();

		int prevRow = -1;

		for (TablePosition position : positionList) {

			int row = position.getRow();
			int col = sequence_column_id;

			Object cell = (Object) table.getColumns().get(col).getCellData(row);

			// null-check: provide empty string for nulls
			if (cell == null) {
				cell = "";
			}

			// determine whether we advance in a row (tab) or a column
			// (newline).
			if (prevRow == row) {
				
				clipboardString.append('\t');
				
			} else if (prevRow != -1) {
				
				clipboardString.append(newline);
				
			}

			// create string from cell
			String text = cell.toString();
			
			// trim primers if required
			if ( !this.showPrimersRadioButton.isSelected() ) {
				
				//get aptamer bounds
				int id = experiment.getAptamerPool().getIdentifier(text);
				AptamerBounds ab = experiment.getAptamerPool().getAptamerBounds(id);
				
				text = text.substring(ab.startIndex, ab.endIndex);
				
			}

			// add new item to clipboard
			clipboardString.append(text);

			// remember previous
			prevRow = row;
		}

		// create clipboard content
		final ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(clipboardString.toString());

		// set clipboard content
		Clipboard.getSystemClipboard().setContent(clipboardContent);
		
	}
	
	/**
	 * Get table selection and copy it to the clipboard.
	 * @param table
	 */
	private void copyRowToClipboard() {

		StringBuilder clipboardString = new StringBuilder();

		ObservableList<TablePosition> positionList = table.getSelectionModel().getSelectedCells();
		
		//Since we have subcolumns, we need to concatenate them first pior to iterating over them 
		ObservableList<TableColumn<Object, ?>> all_columns = FXCollections.observableArrayList();
		for (int col=0; col<table.getColumns().size(); col++) {
			
			if (table.getColumns().get(col).getColumns().size() == 0) {
				
				all_columns.add( (TableColumn<Object, ?>) table.getColumns().get(col) );
				
			}
			else {
				
				for (TableColumn<?, ?> subcol : table.getColumns().get(col).getColumns()) {
				
					all_columns.add((TableColumn<Object, ?>) subcol);
					
				}
				
			}
			
		}
		
		int number_of_columns = all_columns.size();
		int prevRow = -1;

		for (TablePosition position : positionList) {

			int row = position.getRow();
			
			
			// iterate over the columns
			for (int col=0; col<number_of_columns; col++) {
			
				Object cell = (Object) all_columns.get(col).getCellData(row);
	
				// null-check: provide empty string for nulls
				if (cell == null) {
					cell = "";
				}
	
				// determine whether we advance in a row (tab) or a column
				// (newline).
				if (prevRow == row) {
					
					clipboardString.append('\t');
					
				} else if (prevRow != -1) {
					
					clipboardString.append(newline);
					
				}

				// create string from cell
				String text = cell.toString();
				
				// trim primers if required
				if ( col == this.sequence_column_id && !this.showPrimersRadioButton.isSelected() ) {
					
					//get aptamer bounds
					int id = experiment.getAptamerPool().getIdentifier(text);
					AptamerBounds ab = experiment.getAptamerPool().getAptamerBounds(id);
					
					text = text.substring(ab.startIndex, ab.endIndex);
					
				}
	
				// add new item to clipboard
				clipboardString.append(text);
	
				// remember previous
				prevRow = row;
			
			}
		}

		// create clipboard content
		final ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(clipboardString.toString());

		// set clipboard content
		Clipboard.getSystemClipboard().setContent(clipboardContent);
		
	}
	
}