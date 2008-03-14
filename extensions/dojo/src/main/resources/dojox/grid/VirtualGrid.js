if(!dojo._hasResource["dojox.grid.VirtualGrid"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.grid.VirtualGrid"] = true;
dojo.provide("dojox.grid.VirtualGrid");
dojo.require("dojox.grid._grid.lib");
dojo.require("dojox.grid._grid.scroller");
dojo.require("dojox.grid._grid.view");
dojo.require("dojox.grid._grid.views");
dojo.require("dojox.grid._grid.layout");
dojo.require("dojox.grid._grid.rows");
dojo.require("dojox.grid._grid.focus");
dojo.require("dojox.grid._grid.selection");
dojo.require("dojox.grid._grid.edit");
dojo.require("dojox.grid._grid.rowbar");
dojo.require("dojox.grid._grid.publicEvents");

dojo.declare('dojox.VirtualGrid', 
	[ dijit._Widget, dijit._Templated ], 
{
	// summary:
	// 		A grid widget with virtual scrolling, cell editing, complex rows,
	// 		sorting, fixed columns, sizeable columns, etc.
	//
	//	description:
	//		VirtualGrid provides the full set of grid features without any
	//		direct connection to a data store.
	//
	//		The grid exposes a get function for the grid, or optionally
	//		individual columns, to populate cell contents.
	//
	//		The grid is rendered based on its structure, an object describing
	//		column and cell layout.
	//
	//	example:
	//		A quick sample:
	//		
	//		define a get function
	//	|	function get(inRowIndex){ // called in cell context
	//	|		return [this.index, inRowIndex].join(', ');
	//	|	}
	//		
	//		define the grid structure:
	//	|	var structure = [ // array of view objects
	//	|		{ cells: [// array of rows, a row is an array of cells
	//	|			[
	//	|				{ name: "Alpha", width: 6 }, 
	//	|				{ name: "Beta" }, 
	//	|				{ name: "Gamma", get: get }]
	//	|		]}
	//	|	];
	//		
	//	|	<div id="grid" 
	//	|		rowCount="100" get="get" 
	//	|		structure="structure" 
	//	|		dojoType="dojox.VirtualGrid"></div>

	templateString: '<div class="dojoxGrid" hidefocus="hidefocus" role="wairole:grid"><div class="dojoxGrid-master-header" dojoAttachPoint="headerNode"></div><div class="dojoxGrid-master-view" dojoAttachPoint="viewsNode"></div><span dojoAttachPoint="lastFocusNode" tabindex="0"></span></div>',
	// classTag: string
	// css class applied to the grid's domNode
	classTag: 'dojoxGrid',
	get: function(inRowIndex){
		/* summary: Default data getter. 
			description:
				Provides data to display in a grid cell. Called in grid cell context.
				So this.cell.index is the column index.
			inRowIndex: integer
				row for which to provide data
			returns:
				data to display for a given grid cell.
		*/
	},
	// settings
	// rowCount: int
	//	Number of rows to display. 
	rowCount: 5,
	// keepRows: int
	//	Number of rows to keep in the rendering cache.
	keepRows: 75, 
	// rowsPerPage: int
	//	Number of rows to render at a time.
	rowsPerPage: 25,
	// autoWidth: boolean
	//	If autoWidth is true, grid width is automatically set to fit the data.
	autoWidth: false,
	// autoHeight: boolean
	//	If autoHeight is true, grid height is automatically set to fit the data.
	autoHeight: false,
	// autoRender: boolean
	//	If autoRender is true, grid will render itself after initialization.
	autoRender: true,
	// defaultHeight: string
	//	default height of the grid, measured in any valid css unit.
	defaultHeight: '15em',
	// structure: object or string
	//	View layout defintion. Can be set to a layout object, or to the (string) name of a layout object.
	structure: '',
	// elasticView: int
	//	Override defaults and make the indexed grid view elastic, thus filling available horizontal space.
	elasticView: -1,
	// singleClickEdit: boolean
	//	Single-click starts editing. Default is double-click
	singleClickEdit: false,
	// private
	sortInfo: 0,
	themeable: true,
	// initialization
	buildRendering: function(){
		this.inherited(arguments);
		// reset get from blank function (needed for markup parsing) to null, if not changed
		if(this.get == dojox.VirtualGrid.prototype.get){
			this.get = null;
		}
		if(!this.domNode.getAttribute('tabIndex')){
			this.domNode.tabIndex = "0";
		}
		this.createScroller();
		this.createLayout();
		this.createViews();
		this.createManagers();
		dojox.grid.initTextSizePoll();
		this.connect(dojox.grid, "textSizeChanged", "textSizeChanged");
		dojox.grid.funnelEvents(this.domNode, this, 'doKeyEvent', dojox.grid.keyEvents);
		this.connect(this, "onShow", "renderOnIdle");
	},
	postCreate: function(){
		// replace stock styleChanged with one that triggers an update
		this.styleChanged = this._styleChanged;
		this.setStructure(this.structure);
	},
	destroy: function(){
		this.domNode.onReveal = null;
		this.domNode.onSizeChange = null;
		this.edit.destroy();
		this.views.destroyViews();
		this.inherited(arguments);
	},
	styleChanged: function(){
		this.setStyledClass(this.domNode, '');
	},
	_styleChanged: function(){
		this.styleChanged();
		this.update();
	},
	textSizeChanged: function(){
		setTimeout(dojo.hitch(this, "_textSizeChanged"), 1);
	},
	_textSizeChanged: function(){
		if(this.domNode){
			this.views.forEach(function(v){
				v.content.update();
			});
			this.render();
		}
	},
	sizeChange: function(){
		dojox.grid.jobs.job(this.id + 'SizeChange', 50, dojo.hitch(this, "update"));
	},
	renderOnIdle: function() {
		setTimeout(dojo.hitch(this, "render"), 1);
	},
	// managers
	createManagers: function(){
		// summary:
		//	create grid managers for various tasks including rows, focus, selection, editing
		
		// row manager
		this.rows = new dojox.grid.rows(this);
		// focus manager
		this.focus = new dojox.grid.focus(this);
		// selection manager
		this.selection = new dojox.grid.selection(this);
		// edit manager
		this.edit = new dojox.grid.edit(this);
	},
	// virtual scroller
	createScroller: function(){
		this.scroller = new dojox.grid.scroller.columns();
		this.scroller.renderRow = dojo.hitch(this, "renderRow");
		this.scroller.removeRow = dojo.hitch(this, "rowRemoved");
	},
	// layout
	createLayout: function(){
		this.layout = new dojox.grid.layout(this);
	},
	// views
	createViews: function(){
		this.views = new dojox.grid.views(this);
		this.views.createView = dojo.hitch(this, "createView");
	},
	createView: function(inClass){
		var c = eval(inClass);
		var view = new c({ grid: this });
		this.viewsNode.appendChild(view.domNode);
		this.headerNode.appendChild(view.headerNode);
		this.views.addView(view);
		return view;
	},
	buildViews: function(){
		for(var i=0, vs; (vs=this.layout.structure[i]); i++){
			this.createView(vs.type || "dojox.GridView").setStructure(vs);
		}
		this.scroller.setContentNodes(this.views.getContentNodes());
	},
	setStructure: function(inStructure){
		// summary:
		//		Install a new structure and rebuild the grid.
		// inStructure: Object
		//		Structure object defines the grid layout and provides various
		//		options for grid views and columns
		//	description:
		//		A grid structure is an array of view objects. A view object can
		//		specify a view type (view class), width, noscroll (boolean flag
		//		for view scrolling), and cells. Cells is an array of objects
		//		corresponding to each grid column. The view cells object is an
		//		array of subrows comprising a single row. Each subrow is an
		//		array of column objects. A column object can have a name,
		//		width, value (default), get function to provide data, styles,
		//		and span attributes (rowSpan, colSpan).

		this.views.destroyViews();
		this.structure = inStructure;
		if((this.structure)&&(dojo.isString(this.structure))){
			this.structure=dojox.grid.getProp(this.structure);
		}
		if(!this.structure){
			this.structure=window["layout"];
		}
		if(!this.structure){
			return;
		}
		this.layout.setStructure(this.structure);
		this._structureChanged();
	},
	_structureChanged: function() {
		this.buildViews();
		if(this.autoRender){
			this.render();
		}
	},
	// sizing
	resize: function(){
		// summary:
		//		Update the grid's rendering dimensions and resize it
		
		// FIXME: If grid is not sized explicitly, sometimes bogus scrollbars 
		// can appear in our container, which may require an extra call to 'resize'
		// to sort out.
		
		// if we have set up everything except the DOM, we cannot resize
		if(!this.domNode.parentNode){
			return;
		}
		// useful measurement
		var padBorder = dojo._getPadBorderExtents(this.domNode);
		// grid height
		if(this.autoHeight){
			this.domNode.style.height = 'auto';
			this.viewsNode.style.height = '';
		}else if(this.flex > 0){
		}else if(this.domNode.clientHeight <= padBorder.h){
			if(this.domNode.parentNode == document.body){
				this.domNode.style.height = this.defaultHeight;
			}else{
				this.fitTo = "parent";
			}
		}
		if(this.fitTo == "parent"){
			var h = dojo._getContentBox(this.domNode.parentNode).h;
			dojo.marginBox(this.domNode, { h: Math.max(0, h) });
		}
		// header height
		var t = this.views.measureHeader();
		this.headerNode.style.height = t + 'px';
		// content extent
		var l = 1, h = (this.autoHeight ? -1 : Math.max(this.domNode.clientHeight - t, 0) || 0);
		if(this.autoWidth){
			// grid width set to total width
			this.domNode.style.width = this.views.arrange(l, 0, 0, h) + 'px';
		}else{
			// views fit to our clientWidth
			var w = this.domNode.clientWidth || (this.domNode.offsetWidth - padBorder.w);
			this.views.arrange(l, 0, w, h);
		}
		// virtual scroller height
		this.scroller.windowHeight = h; 
		// default row height (FIXME: use running average(?), remove magic #)
		this.scroller.defaultRowHeight = this.rows.getDefaultHeightPx() + 1;
		this.postresize();
	},
	resizeHeight: function(){
		var t = this.views.measureHeader();
		this.headerNode.style.height = t + 'px';
		// content extent
		var h = (this.autoHeight ? -1 : Math.max(this.domNode.clientHeight - t, 0) || 0);
		//this.views.arrange(0, 0, 0, h);
		this.views.onEach('setSize', [0, h]);
		this.views.onEach('resizeHeight');
		this.scroller.windowHeight = h; 
	},
	// render 
	render: function(){
		// summary:
		//	Render the grid, headers, and views. Edit and scrolling states are reset. To retain edit and 
		// scrolling states, see Update.

		if(!this.domNode){return;}
		//
		this.update = this.defaultUpdate;
		this.scroller.init(this.rowCount, this.keepRows, this.rowsPerPage);
		this.prerender();
		this.setScrollTop(0);
		this.postrender();
	},
	prerender: function(){
		this.views.render();
		this.resize();
	},
	postrender: function(){
		this.postresize();
		this.focus.initFocusView();
		// make rows unselectable
		dojo.setSelectable(this.domNode, false);
	},
	postresize: function(){
		// views are position absolute, so they do not inflate the parent
		if(this.autoHeight){
			this.viewsNode.style.height = this.views.measureContent() + 'px';
		}
	},
	// private, used internally to render rows
	renderRow: function(inRowIndex, inNodes){
		this.views.renderRow(inRowIndex, inNodes);
	},
	// private, used internally to remove rows
	rowRemoved: function(inRowIndex){
		this.views.rowRemoved(inRowIndex);
	},
	invalidated: null,
	updating: false,
	beginUpdate: function(){
		// summary:
		//	Use to make multiple changes to rows while queueing row updating.
		// NOTE: not currently supporting nested begin/endUpdate calls
		this.invalidated = [];
		this.updating = true;
	},
	endUpdate: function(){
		// summary:
		//	Use after calling beginUpdate to render any changes made to rows.
		this.updating = false;
		var i = this.invalidated;
		if(i.all){
			this.update();
		}else if(i.rowCount != undefined){
			this.updateRowCount(i.rowCount);
		}else{
			for(r in i){
				this.updateRow(Number(r));
			}
		}
		this.invalidated = null;
	},
	// update
	defaultUpdate: function(){
		// note: initial update calls render and subsequently this function.
		if(this.updating){
			this.invalidated.all = true;
			return;
		}
		//this.edit.saveState(inRowIndex);
		this.prerender();
		this.scroller.invalidateNodes();
		this.setScrollTop(this.scrollTop);
		this.postrender();
		//this.edit.restoreState(inRowIndex);
	},
	update: function(){
		// summary:
		//	Update the grid, retaining edit and scrolling states.
		this.render();
	},
	updateRow: function(inRowIndex){
		// summary:
		//	Render a single row.
		// inRowIndex: int
		//	index of the row to render
		inRowIndex = Number(inRowIndex);
		if(this.updating){
			this.invalidated[inRowIndex]=true;
			return;
		}
		this.views.updateRow(inRowIndex, this.rows.getHeight(inRowIndex));
		this.scroller.rowHeightChanged(inRowIndex);
	},
	updateRowCount: function(inRowCount){
		//summary: 
		//	Change the number of rows.
		// inRowCount: int
		//	Number of rows in the grid.
		if(this.updating){
			this.invalidated.rowCount = inRowCount;
			return;
		}
		this.rowCount = inRowCount;
		this.scroller.updateRowCount(inRowCount);
		this.setScrollTop(this.scrollTop);
		this.resize();
	},
	updateRowStyles: function(inRowIndex){
		// summary:
		//	Update the styles for a row after it's state has changed.
		
		this.views.updateRowStyles(inRowIndex);
	},
	rowHeightChanged: function(inRowIndex){
		// summary: 
		//	Update grid when the height of a row has changed. Row height is handled automatically as rows
		//	are rendered. Use this function only to update a row's height outside the normal rendering process.
		// inRowIndex: int
		// index of the row that has changed height
		
		this.views.renormalizeRow(inRowIndex);
		this.scroller.rowHeightChanged(inRowIndex);
	},
	// fastScroll: boolean
	//	flag modifies vertical scrolling behavior. Defaults to true but set to false for slower 
	//	scroll performance but more immediate scrolling feedback
	fastScroll: true,
	delayScroll: false,
	// scrollRedrawThreshold: int
	//	pixel distance a user must scroll vertically to trigger grid scrolling.
	scrollRedrawThreshold: (dojo.isIE ? 100 : 50),
	// scroll methods
	scrollTo: function(inTop){
		// summary:
		//	Vertically scroll the grid to a given pixel position
		// inTop: int
		//	vertical position of the grid in pixels
		if(!this.fastScroll){
			this.setScrollTop(inTop);
			return;
		}
		var delta = Math.abs(this.lastScrollTop - inTop);
		this.lastScrollTop = inTop;
		if(delta > this.scrollRedrawThreshold || this.delayScroll){
			this.delayScroll = true;
			this.scrollTop = inTop;
			this.views.setScrollTop(inTop);
			dojox.grid.jobs.job('dojoxGrid-scroll', 200, dojo.hitch(this, "finishScrollJob"));
		}else{
			this.setScrollTop(inTop);
		}
	},
	finishScrollJob: function(){
		this.delayScroll = false;
		this.setScrollTop(this.scrollTop);
	},
	setScrollTop: function(inTop){
		this.scrollTop = this.views.setScrollTop(inTop);
		this.scroller.scroll(this.scrollTop);
	},
	scrollToRow: function(inRowIndex){
		// summary:
		//	Scroll the grid to a specific row.
		// inRowIndex: int
		// grid row index
		this.setScrollTop(this.scroller.findScrollTop(inRowIndex) + 1);
	},
	// styling (private, used internally to style individual parts of a row)
	styleRowNode: function(inRowIndex, inRowNode){
		if(inRowNode){
			this.rows.styleRowNode(inRowIndex, inRowNode);
		}
	},
	// cells
	getCell: function(inIndex){
		// summary:
		//	retrieves the cell object for a given grid column.
		// inIndex: int
		// grid column index of cell to retrieve
		// returns:
		//	a grid cell
		return this.layout.cells[inIndex];
	},
	setCellWidth: function(inIndex, inUnitWidth) {
		this.getCell(inIndex).unitWidth = inUnitWidth;
	},
	getCellName: function(inCell){
		return "Cell " + inCell.index;
	},
	// sorting
	canSort: function(inSortInfo){
		// summary:
		//	determines if the grid can be sorted
		// inSortInfo: int
		//	Sort information, 1-based index of column on which to sort, positive for an ascending sort
		// and negative for a descending sort
		// returns:
		//	true if grid can be sorted on the given column in the given direction
	},
	sort: function(){
	},
	getSortAsc: function(inSortInfo){
		// summary:
		//	returns true if grid is sorted in an ascending direction.
		inSortInfo = inSortInfo == undefined ? this.sortInfo : inSortInfo;
		return Boolean(inSortInfo > 0);
	},
	getSortIndex: function(inSortInfo){
		// summary:
		//	returns the index of the column on which the grid is sorted
		inSortInfo = inSortInfo == undefined ? this.sortInfo : inSortInfo;
		return Math.abs(inSortInfo) - 1;
	},
	setSortIndex: function(inIndex, inAsc){
		// summary:
		// Sort the grid on a column in a specified direction
		// inIndex: int
		// Column index on which to sort.
		// inAsc: boolean
		// If true, sort the grid in ascending order, otherwise in descending order
		var si = inIndex +1;
		if(inAsc != undefined){
			si *= (inAsc ? 1 : -1);
		} else if(this.getSortIndex() == inIndex){
			si = -this.sortInfo;
		}
		this.setSortInfo(si);
	},
	setSortInfo: function(inSortInfo){
		if(this.canSort(inSortInfo)){
			this.sortInfo = inSortInfo;
			this.sort();
			this.update();
		}
	},
	// DOM event handler
	doKeyEvent: function(e){
		e.dispatch = 'do' + e.type;
		this.onKeyEvent(e);
	},
	// event dispatch
	//: protected
	_dispatch: function(m, e){
		if(m in this){
			return this[m](e);
		}
	},
	dispatchKeyEvent: function(e){
		this._dispatch(e.dispatch, e);
	},
	dispatchContentEvent: function(e){
		this.edit.dispatchEvent(e) || e.sourceView.dispatchContentEvent(e) || this._dispatch(e.dispatch, e);
	},
	dispatchHeaderEvent: function(e){
		e.sourceView.dispatchHeaderEvent(e) || this._dispatch('doheader' + e.type, e);
	},
	dokeydown: function(e){
		this.onKeyDown(e);
	},
	doclick: function(e){
		if(e.cellNode){
			this.onCellClick(e);
		}else{
			this.onRowClick(e);
		}
	},
	dodblclick: function(e){
		if(e.cellNode){
			this.onCellDblClick(e);
		}else{
			this.onRowDblClick(e);
		}
	},
	docontextmenu: function(e){
		if(e.cellNode){
			this.onCellContextMenu(e);
		}else{
			this.onRowContextMenu(e);
		}
	},
	doheaderclick: function(e){
		if(e.cellNode){
			this.onHeaderCellClick(e);
		}else{
			this.onHeaderClick(e);
		}
	},
	doheaderdblclick: function(e){
		if(e.cellNode){
			this.onHeaderCellDblClick(e);
		}else{
			this.onHeaderDblClick(e);
		}
	},
	doheadercontextmenu: function(e){
		if(e.cellNode){
			this.onHeaderCellContextMenu(e);
		}else{
			this.onHeaderContextMenu(e);
		}
	},
	// override to modify editing process
	doStartEdit: function(inCell, inRowIndex){
		this.onStartEdit(inCell, inRowIndex);
	},
	doApplyCellEdit: function(inValue, inRowIndex, inFieldIndex){
		this.onApplyCellEdit(inValue, inRowIndex, inFieldIndex);
	},
	doCancelEdit: function(inRowIndex){
		this.onCancelEdit(inRowIndex);
	},
	doApplyEdit: function(inRowIndex){
		this.onApplyEdit(inRowIndex);
	},
	// row editing
	addRow: function(){
		// summary:
		//	add a row to the grid.
		this.updateRowCount(this.rowCount+1);
	},
	removeSelectedRows: function(){
		// summary:
		//	remove the selected rows from the grid.
		this.updateRowCount(Math.max(0, this.rowCount - this.selection.getSelected().length));
		this.selection.clear();
	}
});

dojo.mixin(dojox.VirtualGrid.prototype, dojox.grid.publicEvents);

}
