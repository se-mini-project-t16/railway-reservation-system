// Runs before the stylesheet paints: applies the saved light/dark choice so the page never flashes.
(function () {
  try {
    var mode = localStorage.getItem('rrs.mode');
    if (mode === 'light' || mode === 'dark') document.documentElement.setAttribute('data-mode', mode);
  } catch (e) { /* storage blocked: follow the system setting */ }
  document.documentElement.classList.add('js');
})();
