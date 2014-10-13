(function( $ ){
  $.fn.breadcrumbs = function(icon) {
	$.breadcrumbs  = {};
  
    this.addClass("breadcrumb_container");
	
	if (icon)
	{
		this.children().first().before("<li><span class='breadcrumb_e breadcrumb_icon_" + icon + "'></span></li>");
	}
	
	$.breadcrumbs.tc = this.children();
	$.breadcrumbs.tc.addClass("breadcrumb_e breadcrumb_item");
	$.breadcrumbs.tc.after("<li class='breadcrumb_e breadcrumb_divider'>&nbsp;</li>");
	
	$.breadcrumbs.tc = this.children();
	$.breadcrumbs.tc.last().removeClass("breadcrumb_divider").addClass("breadcrumb_end");
	$.breadcrumbs.tc.first().before("<li class='breadcrumb_e breadcrumb_before'>&nbsp;</li>");
	
	$.breadcrumbs.tc = this.children(".breadcrumb_item")
	$.breadcrumbs.tc.hover(bc_menter, bc_mleave);
	$.breadcrumbs.tc.mousedown(bc_mleave);
	$.breadcrumbs.tc.mouseup(bc_menter);
  };
})( jQuery );

function bc_menter()
{
	$(this).addClass("breadcrumb_item_hover");
		
	var next = $(this).next();
	var prev = $(this).prev();
	
	if (next.hasClass("breadcrumb_end")) {next.addClass("breadcrumb_end_hover");}
	if (next.hasClass("breadcrumb_divider")) {next.addClass("breadcrumb_divider_left");}
	if (prev.hasClass("breadcrumb_divider")) {prev.addClass("breadcrumb_divider_right");}
	if (prev.hasClass("breadcrumb_before")) {prev.addClass("breadcrumb_before_hover");}
}

function bc_mleave()
{
	$(this).removeClass("breadcrumb_item_hover");
		
	var next = $(this).next();
	var prev = $(this).prev();
	
	if (next.hasClass("breadcrumb_end_hover")) {next.removeClass("breadcrumb_end_hover");}
	if (next.hasClass("breadcrumb_divider_left")) {next.removeClass("breadcrumb_divider_left");}
	if (prev.hasClass("breadcrumb_divider_right")) {prev.removeClass("breadcrumb_divider_right");}
	if (prev.hasClass("breadcrumb_before_hover")) {prev.removeClass("breadcrumb_before_hover");}
}
