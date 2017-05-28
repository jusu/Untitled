//
// return document height by finding the bottom-most div's coordinates
//

(function() {
  var divs = document.getElementsByTagName("div");
  var i = divs.length || 0;
  var height = 0;

  function elementBottom(el) {
    var box = el.getBoundingClientRect();
    return box.top + box.height + window.pageYOffset - el.clientTop;
  }

  while(i--) {
    height = Math.max(height, elementBottom(divs[i]));
  }

  // failsafe
  height = height || elementBottom(document.body);

  // final failsafe
  height = height || window.getComputedStyle(document.body, null).getPropertyValue('height');

  return height;
})()
