if(!dojo._hasResource["dojox.grid.Grid"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid.Grid"] = true;
dojo.provide("dojox.grid.Grid");
dojo.require("dojox.grid.VirtualGrid");
dojo.require("dojox.grid._data.model");
dojo.require("dojox.grid._data.editors");

dojo.declare('dojox.Grid', dojox.VirtualGrid, {
	//	summary:
	//		A grid widget with virtual scrolling, cell editing, complex rows,
	//		sorting, fixed columns, sizeable columns, etc.
	//	description:
	//		Grid is a subclass of VirtualGrid, providing binding to a data
	//		store.
	//	example:
	//		define the grid structure:
	//	|	var structure = [ // array of view objects
	//	|		{ cells: [// array of rows, a row is an array of cells
	//	|			[	{ name: "Alpha", width: 6 }, 
	//	|				{ name: "Beta" }, 
	//	|				{ name: "Gamma", get: formatFunction }
	//	|			]
	//	|		]}
	//	|	];
	//	  	
	//		define a grid data model
	//	|	var model = new dojox.grid.data.table(null, data);
	//	|
	//	|	<div id="grid" model="model" structure="structure" 
	//	|		dojoType="dojox.VirtualGrid"></div>
	//	

	//	model:
	//		string or object grid data model
	model: 'dojox.grid.data.Table',
	// life cycle
	postCreate: function(){
		if(this.model){
			var m = this.model;
			if(dojo.isString(m)){
				m = dojo.getObject(m);
			}
			this.model = (dojo.isFunction(m)) ? new m() : m;
			this._setModel(this.model);
		}
		this.inherited(arguments);
	},
	destroy: function(){
		this.setModel(null);
		this.inherited(arguments);
	},
	// structure
	_structureChanged: function() {
		this.indexCellFields();
		this.inherited(arguments);
	},
	// model
	_setModel: function(inModel){
		// if(!inModel){ return; }
		this.model = inModel;
		if(this.model){
			this.model.observer(this);
			this.model.measure();
			this.indexCellFields();
		}
	},
	setModel: function(inModel){
		// summary:
		//		set the grid's data model
		// inModel:
		//		model object, usually an instance of a dojox.grid.data.Model
		//		subclass
		if(this.model){
			this.model.notObserver(this);
		}
		this._setModel(inModel);
	},
	// data socket (called in cell's context)
	get: function(inRowIndex){
		return this.grid.model.getDatum(inRowIndex, this.fieldIndex);
	},
	// model modifications
	modelAllChange: function(){
		this.rowCount = (this.model ? this.model.getRowCount() : 0);
		this.updateRowCount(this.rowCount);
	},
	modelRowChange: function(inData, inRowIndex){
		this.updateRow(inRowIndex);
	},
	modelDatumChange: function(inDatum, inRowIndex, inFieldIndex){
		this.updateRow(inRowIndex);
	},
	modelFieldsChange: function() {
		this.indexCellFields();
		this.render();
	},
	// model insertion
	modelInsertion: function(inRowIndex){
		this.updateRowCount(this.model.getRowCount());
	},
	// model removal
	modelRemoval: function(inKeys){
		this.updateRowCount(this.model.getRowCount());
	},
	// cells
	getCellName: function(inCell){
		var v = this.model.fields.values, i = inCell.fieldIndex;
		return i>=0 && i<v.length && v[i].name || this.inherited(arguments);
	},
	indexCellFields: function(){
		var cells = this.layout.cells;
		for(var i=0, c; cells && (c=cells[i]); i++){
			if(dojo.isString(c.field)){
				c.fieldIndex = this.model.fields.indexOf(c.field);
			}
		}
	},
	// utility
	refresh: function(){
		// summary:
		//	re-render the grid, getting new data from the model
		this.edit.cancel();
		this.model.measure();
	},
	// sorting
	canSort: function(inSortInfo){
		var f = this.getSortField(inSortInfo);
		// 0 is not a valid sort field
		return f && this.model.canSort(f);
	},
	getSortField: function(inSortInfo){
		// summary:
		//	retrieves the model field on which to sort data.
		// inSortInfo: int
		//	1-based grid column index; positive if sort is ascending, otherwise negative
		var c = this.getCell(this.getSortIndex(inSortInfo));
		// we expect c.fieldIndex == -1 for non model fields
		// that yields a getSortField value of 0, which can be detected as invalid
		return (c.fieldIndex+1) * (this.sortInfo > 0 ? 1 : -1);
	},
	sort: function(){
		this.edit.apply();
		this.model.sort(this.getSortField());
	},
	// row editing
	addRow: function(inRowData, inIndex){
		this.edit.apply();
		var i = inIndex || -1;
		if(i<0){
			i = this.selection.getFirstSelected() || 0;
		}
		if(i<0){
			i = 0;
		}
		this.model.insert(inRowData, i);
		this.model.beginModifyRow(i);
		// begin editing row
		// FIXME: add to edit
		for(var j=0, c; ((c=this.getCell(j)) && !c.editor); j++){}
		if(c&&c.editor){
			this.edit.setEditCell(c, i);
		}
	},
	removeSelectedRows: function(){
		this.edit.apply();
		var s = this.selection.getSelected();
		if(s.length){
			this.model.remove(s);
			this.selection.clear();
		}
	},
	//: protected
	// editing
	canEdit: function(inCell, inRowIndex){
		// summary: 
		//	determines if a given cell may be edited
		//	inCell: grid cell
		// inRowIndex: grid row index
		// returns: true if given cell may be edited
		return (this.model.canModify ? this.model.canModify(inRowIndex) : true);
	},
	doStartEdit: function(inCell, inRowIndex){
		var edit = this.canEdit(inCell, inRowIndex);
		if(edit){
			this.model.beginModifyRow(inRowIndex);
			this.onStartEdit(inCell, inRowIndex);
		}
		return edit;
	},
	doApplyCellEdit: function(inValue, inRowIndex, inFieldIndex){
		this.model.setDatum(inValue, inRowIndex, inFieldIndex);
		this.onApplyCellEdit(inValue, inRowIndex, inFieldIndex);
	},
	doCancelEdit: function(inRowIndex){
		this.model.cancelModifyRow(inRowIndex);
		this.onCancelEdit.apply(this, arguments);
	},
	doApplyEdit: function(inRowIndex){
		this.model.endModifyRow(inRowIndex);
		this.onApplyEdit(inRowIndex);
	},
	// Perform row styling 
	styleRowState: function(inRow){
		if(this.model.getState){
			var states=this.model.getState(inRow.index), c='';
			for(var i=0, ss=["inflight", "error", "inserting"], s; s=ss[i]; i++){
				if(states[s]){
					c = ' dojoxGrid-row-' + s;
					break;
				}
			}
			inRow.customClasses += c;
		}
	},
	onStyleRow: function(inRow){
		this.styleRowState(inRow);
		this.inherited(arguments);
	},
	junk: 0
});

}
