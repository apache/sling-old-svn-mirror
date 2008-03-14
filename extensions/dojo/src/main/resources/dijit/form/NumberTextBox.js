if(!dojo._hasResource["dijit.form.NumberTextBox"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dijit.form.NumberTextBox"] = true;
dojo.provide("dijit.form.NumberTextBox");

dojo.require("dijit.form.ValidationTextBox");
dojo.require("dojo.number");

dojo.declare(
	"dijit.form.NumberTextBoxMixin",
	null,
	{
		// summary:
		//		A mixin for all number textboxes
		regExpGen: dojo.number.regexp,

		format: function(/*Number*/ value, /*Object*/ constraints){
			if(isNaN(value)){ return ""; }
			return dojo.number.format(value, constraints);
		},

		parse: dojo.number.parse,

		filter: function(/*Number*/ value){
			if(typeof value == "string"){ return this.inherited('filter', arguments); }
			return (isNaN(value) ? '' : value);
		},

		value: NaN
	}
);

dojo.declare(
	"dijit.form.NumberTextBox",
	[dijit.form.RangeBoundTextBox,dijit.form.NumberTextBoxMixin],
	{
		// summary:
		//		A validating, serializable, range-bound text box.
		// constraints object: min, max, places
	}
);

}
