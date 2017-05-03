$(document).ready(function(){
	if (window.location.hash) {
		$('.page-navigation__item').removeClass('current');
		$('.page-navigation__item[href=' + window.location.hash + ']').addClass('current');
	}

	if (document.getElementById('ui-slider')) {
		$('#ui-slider').slider({
			min: 0,
			max: 1000,
			step: 100,
			value: 200,
			create: function( event, ui ) {
				$('#area').text($('#ui-slider').slider('value') + ' м');
			},
			slide: function( event, ui ) {
				$('#area').text(ui.value + ' м');
			}
		});
	}

	$('.page-navigation__item').on('click', function() {
		$(this).addClass('current');
		$(this).siblings().removeClass('current');
	});

	$('.search-input').on('keyup', function() {
		if ($(this).val().length) $(this).parent().addClass('add');
		else $(this).parent().removeClass('add');
	});
});

$(window).scroll(function() {
	var scrollTop = $(window).scrollTop();
	var sections = $('.section');
	for (var i = 0; i < sections.length; i++) {
		if (scrollTop >= $(sections[i]).offset().top) {
			$('.page-navigation__item').removeClass('current');
			$('.page-navigation__item[href=#' + $(sections[i]).attr('id') + ']').addClass('current');
		}
	}
});
