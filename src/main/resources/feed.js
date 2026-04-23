(function() {
  var liveDot   = document.getElementById('live-dot');
  var liveLabel = document.getElementById('live-label');
  var pauseBtn  = document.getElementById('pause-btn');
  var msgCount  = document.getElementById('msg-count');
  var searchInput = document.getElementById('search-input');
  var feed      = document.getElementById('feed');

  var paused = false;
  var activeFilter = '';

  function updateCount() {
    var rows = feed.querySelectorAll('[data-feed-row]');
    var visible = Array.from(rows).filter(function(r) { return r.style.display !== 'none'; }).length;
    msgCount.textContent = visible + ' msg' + (visible !== 1 ? 's' : '');
  }

  function applyFilters() {
    var q = searchInput.value.toLowerCase();
    var rows = feed.querySelectorAll('[data-feed-row]');
    rows.forEach(function(row) {
      var matchSrc = !activeFilter || row.dataset.source === activeFilter;
      var matchSearch = !q || row.textContent.toLowerCase().includes(q);
      row.style.display = (matchSrc && matchSearch) ? '' : 'none';
    });
    updateCount();
  }

  document.body.addEventListener('htmx:sseOpen', function() {
    liveDot.style.background = 'hsl(var(--primary))';
    liveDot.style.animation = 'pulse 2s ease-in-out infinite';
    liveLabel.textContent = 'LIVE';
    liveLabel.style.color = 'hsl(var(--primary))';
  });
  document.body.addEventListener('htmx:sseError', function() {
    liveDot.style.background = 'hsl(var(--destructive))';
    liveDot.style.animation = 'none';
    liveLabel.textContent = 'ERROR';
    liveLabel.style.color = 'hsl(var(--destructive))';
  });

  function updateSidebarCounts() {
    var rows = feed.querySelectorAll('[data-feed-row]');
    var counts = {};
    rows.forEach(function(r) {
      var s = r.dataset.source;
      counts[s] = (counts[s] || 0) + 1;
    });
    var allItem = document.querySelector('.src-item[data-source-filter=""]');
    if (allItem) allItem.querySelector('span:last-child').textContent = rows.length;
    var sourcesSection = allItem && allItem.parentElement;
    var existingSources = new Set();
    document.querySelectorAll('.src-item[data-source-filter]').forEach(function(item) {
      var s = item.dataset.sourceFilter;
      if (!s) return;
      existingSources.add(s);
      item.querySelector('span:last-child').textContent = counts[s] || 0;
    });
    if (sourcesSection) {
      Object.keys(counts).forEach(function(src) {
        if (existingSources.has(src)) return;
        var sampleRow = feed.querySelector('[data-source="' + src + '"]');
        var color = sampleRow ? sampleRow.querySelector('span').style.color : '#888';
        var label = src.toUpperCase();
        if (label.length > 10) label = label.substring(0, 9) + '…';
        var item = document.createElement('div');
        item.className = 'src-item';
        item.dataset.sourceFilter = src;
        item.innerHTML = '<span class="src-label" style="font-size:11px;color:' + color + ';overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:120px">' + label + '</span>' +
          '<span style="font-size:10px;color:hsl(var(--muted-foreground))">' + counts[src] + '</span>';
        item.addEventListener('click', function() {
          var s = item.dataset.sourceFilter;
          activeFilter = (activeFilter === s && s !== '') ? '' : s;
          document.querySelectorAll('.src-item').forEach(function(i) {
            i.classList.toggle('active', i.dataset.sourceFilter === activeFilter);
          });
          applyFilters();
        });
        sourcesSection.appendChild(item);
      });
    }
  }

  document.body.addEventListener('htmx:sseMessage', function() {
    document.getElementById('empty-state') && document.getElementById('empty-state').remove();
    applyFilters();
    updateSidebarCounts();
  });

  pauseBtn.addEventListener('click', function() {
    paused = !paused;
    pauseBtn.textContent = paused ? 'RESUME' : 'PAUSE';
    pauseBtn.style.color = paused ? 'hsl(var(--primary))' : 'hsl(var(--muted-foreground))';
    liveDot.style.animation = paused ? 'none' : 'pulse 2s ease-in-out infinite';
    liveLabel.textContent = paused ? 'PAUSED' : 'LIVE';
  });

  searchInput.addEventListener('input', applyFilters);

  document.querySelectorAll('.src-item').forEach(function(item) {
    item.addEventListener('click', function() {
      var src = item.dataset.sourceFilter;
      activeFilter = (activeFilter === src && src !== '') ? '' : src;
      document.querySelectorAll('.src-item').forEach(function(i) {
        i.classList.toggle('active', i.dataset.sourceFilter === activeFilter);
      });
      applyFilters();
    });
  });

  updateCount();
})();
