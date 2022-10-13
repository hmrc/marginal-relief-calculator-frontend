// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.getElementById('save').addEventListener('click', function(e){
    var element = document.getElementById('pdf-preview');
    var clone = element.cloneNode(true);
    clone.classList.add('save');
    var m = new Date();
    var filename = "marginal-relief-for-corporation-tax-result-" + m.getUTCDate()+(m.getUTCMonth()+1)+m.getUTCFullYear()+"-"+ m.getUTCHours()+String(m.getUTCMinutes()).padStart(2, '0')+".pdf";

    var opt = {
      filename:     filename
    };

    html2pdf(clone, opt);

});
