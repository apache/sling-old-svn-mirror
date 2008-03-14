if(!dojo._hasResource["dojox.grid._grid.publicEvents"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid._grid.publicEvents"] = true;
dojo.provide("dojox.grid._grid.publicEvents");

dojox.grid.publicEvents = {
	// summary:
	//	VirtualGrid mixin that provides default implementations for grid events.
	//	dojo.connect to events to retain default implementation or override them for custom handling.
	
	//cellOverClass: string
	// css class to apply to grid cells over which the cursor is placed.
	cellOverClass: "dojoxGrid-cell-over",
	// top level handlers (more specified handlers below)
	onKeyEvent: function(e){
		this.dispatchKeyEvent(e);
	},
	onContentEvent: function(e){
		this.dispatchContentEvent(e);
	},
	onHeaderEvent: function(e){
		this.dispatchHeaderEvent(e);
	},
	onStyleRow: function(inRow){
		// summary:
		//	Perform row styling on a given row. Called whenever row styling is updated.
		// inRow: object
		// Object containing row state information: selected, true if the row is selcted; over:
		// true of the mouse is over the row; odd: true if the row is odd. Use customClasses and
		// customStyles to control row css classes and styles; both properties are strings.
		with(inRow){
			customClasses += (odd?" dojoxGrid-row-odd":"") + (selected?" dojoxGrid-row-selected":"") + (over?" dojoxGrid-row-over":"");
		}
		this.focus.styleRow(inRow);
		this.edit.styleRow(inRow);
	},
	onKeyDown: function(e){
		// summary:
		// grid key event handler. By default enter begins editing and applies edits, escape cancels and edit,
		// tab, shift-tab, and arrow keys move grid cell focus.
		if(e.altKey || e.ctrlKey || e.metaKey ){
			return;
		}
		switch(e.keyCode){
			case dojo.keys.ESCAPE:
				this.edit.cancel();
				break;
			case dojo.keys.ENTER:
				if (!e.shiftKey) {
					var isEditing = this.edit.isEditing();
					this.edit.apply();
					if(!isEditing){
						this.edit.setEditCell(this.focus.cell, this.focus.rowIndex);
					}
				}
				break;
			case dojo.keys.TAB:
				this.focus[e.shiftKey ? 'previousKey' : 'nextKey'](e);
				break;
			case dojo.keys.LEFT_ARROW:
				if(!this.edit.isEditing()){
				this.focus.move(0, -1);
				}
				break;
			case dojo.keys.RIGHT_ARROW:
				if(!this.edit.isEditing()){
					this.focus.move(0, 1);
				}
				break;
			case dojo.keys.UP_ARROW:
				if(!this.edit.isEditing()){
					this.focus.move(-1, 0);
				}
				break;
			case dojo.keys.DOWN_ARROW:
				if(!this.edit.isEditing()){
					this.focus.move(1, 0);
				}
				break;
		}
	},
	onMouseOver: function(e){
		// summary:
		//	event fired when mouse is over the grid.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		e.rowIndex == -1 ? this.onHeaderCellMouseOver(e) : this.onCellMouseOver(e);
	},
	onMouseOut: function(e){
		// summary:
		//	event fired when mouse moves out of the grid.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		e.rowIndex == -1 ? this.onHeaderCellMouseOut(e) : this.onCellMouseOut(e);
	},
	onMouseOverRow: function(e){
		// summary:
		//	event fired when mouse is over any row (data or header).
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		if(!this.rows.isOver(e.rowIndex)){
			this.rows.setOverRow(e.rowIndex);
			e.rowIndex == -1 ? this.onHeaderMouseOver(e) : this.onRowMouseOver(e);
		}
	},
	onMouseOutRow: function(e){
		// summary:
		//	event fired when mouse moves out of any row (data or header).
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		if(this.rows.isOver(-1)){
			this.onHeaderMouseOut(e);
		}else if(!this.rows.isOver(-2)){
			this.rows.setOverRow(-2);
			this.onRowMouseOut(e);
		}
	},
	// cell events
	onCellMouseOver: function(e){
		// summary:
		//	event fired when mouse is over a cell.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.addClass(e.cellNode, this.cellOverClass);
	},
	onCellMouseOut: function(e){
		// summary:
		//	event fired when mouse moves out of a cell.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.removeClass(e.cellNode, this.cellOverClass);
	},
	onCellClick: function(e){
		// summary:
		//	event fired when a cell is clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.focus.setFocusCell(e.cell, e.rowIndex);
		this.onRowClick(e);
	},
	onCellDblClick: function(e){
		// summary:
		//	event fired when a cell is double-clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.edit.setEditCell(e.cell, e.rowIndex); 
		this.onRowDblClick(e);
	},
	onCellContextMenu: function(e){
		// summary:
		//	event fired when a cell context menu is accessed via mouse right click.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.onRowContextMenu(e);
	},
	onCellFocus: function(inCell, inRowIndex){
		// summary:
		//	event fired when a cell receives focus.
		// inCell: object
		//	cell object containing properties of the grid column.
		// inRowIndex: int
		//	index of the grid row
		this.edit.cellFocus(inCell, inRowIndex);
	},
	// row events
	onRowClick: function(e){
		// summary:
		//	event fired when a row is clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.edit.rowClick(e);
		this.selection.clickSelectEvent(e);
	},
	onRowDblClick: function(e){
		// summary:
		//	event fired when a row is double clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onRowMouseOver: function(e){
		// summary:
		//	event fired when mouse moves over a data row.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onRowMouseOut: function(e){
		// summary:
		//	event fired when mouse moves out of a data row.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onRowContextMenu: function(e){
		// summary:
		//	event fired when a row context menu is accessed via mouse right click.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.stopEvent(e);
	},
	// header events
	onHeaderMouseOver: function(e){
		// summary:
		//	event fired when mouse moves over the grid header.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onHeaderMouseOut: function(e){
		// summary:
		//	event fired when mouse moves out of the grid header.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onHeaderCellMouseOver: function(e){
		// summary:
		//	event fired when mouse moves over a header cell.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.addClass(e.cellNode, this.cellOverClass);
	},
	onHeaderCellMouseOut: function(e){
		// summary:
		//	event fired when mouse moves out of a header cell.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.removeClass(e.cellNode, this.cellOverClass);
	},
	onHeaderClick: function(e){
		// summary:
		//	event fired when the grid header is clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onHeaderCellClick: function(e){
		// summary:
		//	event fired when a header cell is clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.setSortIndex(e.cell.index);
		this.onHeaderClick(e);
	},
	onHeaderDblClick: function(e){
		// summary:
		//	event fired when the grid header is double clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
	},
	onHeaderCellDblClick: function(e){
		// summary:
		//	event fired when a header cell is double clicked.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.onHeaderDblClick(e);
	},
	onHeaderCellContextMenu: function(e){
		// summary:
		//	event fired when a header cell context menu is accessed via mouse right click.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		this.onHeaderContextMenu(e);
	},
	onHeaderContextMenu: function(e){
		// summary:
		//	event fired when the grid header context menu is accessed via mouse right click.
		// e: decorated event object 
		//	contains reference to grid, cell, and rowIndex
		dojo.stopEvent(e);
	},
	// editing
	onStartEdit: function(inCell, inRowIndex){
		// summary:
		//	event fired when editing is started for a given grid cell
		// inCell: object
		//	cell object containing properties of the grid column.
		// inRowIndex: int
		//	index of the grid row
	},
	onApplyCellEdit: function(inValue, inRowIndex, inFieldIndex){
		// summary:
		//	event fired when editing is applied for a given grid cell
		// inValue: string
		//	value from cell editor
		// inRowIndex: int
		//	index of the grid row
		// inFieldIndex: int
		//	index in the grid's data model
	},
	onCancelEdit: function(inRowIndex){
		// summary:
		//	event fired when editing is cancelled for a given grid cell
		// inRowIndex: int
		//	index of the grid row
	},
	onApplyEdit: function(inRowIndex){
		// summary:
		//	event fired when editing is applied for a given grid row
		// inRowIndex: int
		//	index of the grid row
	},
	onCanSelect: function(inRowIndex){
		// summary:
		//	event to determine if a grid row may be selected
		// inRowIndex: int
		//	index of the grid row
		// returns:
		//	true if the row can be selected
		return true // boolean;
	},
	onCanDeselect: function(inRowIndex){
		// summary:
		//	event to determine if a grid row may be deselected
		// inRowIndex: int
		//	index of the grid row
		// returns:
		//	true if the row can be deselected
		return true // boolean;
	},
	onSelected: function(inRowIndex){
		// summary:
		//	event fired when a grid row is selected
		// inRowIndex: int
		//	index of the grid row
		this.updateRowStyles(inRowIndex);
	},
	onDeselected: function(inRowIndex){
		// summary:
		//	event fired when a grid row is deselected
		// inRowIndex: int
		//	index of the grid row
		this.updateRowStyles(inRowIndex);
	},
	onSelectionChanged: function(){
	}
}

}
