// Forge 3D Reactor Scene
(function() {
    var canvas = document.getElementById('forge-canvas');
    if (!canvas) return;

    var ctx = canvas.getContext('2d');
    if (!ctx) {
        if (window.ForgeBridge && window.ForgeBridge.onWebGLFailed) {
            window.ForgeBridge.onWebGLFailed();
        }
        return;
    }

    var width = window.innerWidth;
    var height = window.innerHeight;
    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    var progress = 0;
    var stageName = "INITIALIZING";
    var rotation = 0;
    var particles = [];

    // Initialize 80 glyph particles spiraling into core
    for (var i = 0; i < 80; i++) {
        particles.push({
            angle: Math.random() * Math.PI * 2,
            radius: 80 + Math.random() * 200,
            speed: 0.01 + Math.random() * 0.02,
            size: 2 + Math.random() * 3,
            glyph: ['{', '}', '<', '>', '/', '=', ';', '#', '$'][Math.floor(Math.random() * 9)],
            alpha: 0.2 + Math.random() * 0.8
        });
    }

    function render() {
        ctx.fillStyle = '#06050E';
        ctx.fillRect(0, 0, width, height);

        var cx = width / 2;
        var cy = height / 2;

        rotation += 0.015;

        // Draw radial background gradient
        var grad = ctx.createRadialGradient(cx, cy, 10, cx, cy, width * 0.7);
        grad.addColorStop(0, '#1E174D');
        grad.addColorStop(0.5, '#0E0B24');
        grad.addColorStop(1, '#06050E');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, width, height);

        // Core pulsating sphere
        var corePulse = 38 + Math.sin(rotation * 3) * 6 + (progress * 0.2);
        var coreGrad = ctx.createRadialGradient(cx, cy, 2, cx, cy, corePulse);
        coreGrad.addColorStop(0, '#FFFFFF');
        coreGrad.addColorStop(0.4, '#FFB547');
        coreGrad.addColorStop(0.8, '#FF6FAE');
        coreGrad.addColorStop(1, 'rgba(255, 111, 174, 0)');
        ctx.fillStyle = coreGrad;
        ctx.beginPath();
        ctx.arc(cx, cy, corePulse * 1.6, 0, Math.PI * 2);
        ctx.fill();

        // 3 Gyroscope Orbit Rings
        for (var r = 0; r < 3; r++) {
            ctx.save();
            ctx.translate(cx, cy);
            ctx.rotate(rotation * (r % 2 === 0 ? 1 : -1) + (r * Math.PI / 3));
            ctx.scale(1, 0.45);
            ctx.strokeStyle = r === 0 ? '#3FE0D0' : (r === 1 ? '#FFB547' : '#FF6FAE');
            ctx.lineWidth = 2.5;
            ctx.beginPath();
            ctx.arc(0, 0, 75 + r * 30, 0, Math.PI * 2);
            ctx.stroke();
            ctx.restore();
        }

        // Particle stream spiraling toward the core
        ctx.fillStyle = '#FFB547';
        ctx.font = '12px monospace';
        for (var p = 0; p < particles.length; p++) {
            var pt = particles[p];
            pt.angle += pt.speed;
            pt.radius -= 0.4;
            if (pt.radius < 20) pt.radius = 180 + Math.random() * 80;

            var px = cx + Math.cos(pt.angle) * pt.radius;
            var py = cy + Math.sin(pt.angle) * (pt.radius * 0.7);

            ctx.globalAlpha = pt.alpha;
            ctx.fillText(pt.glyph, px, py);
        }
        ctx.globalAlpha = 1.0;

        requestAnimationFrame(render);
    }

    render();

    window.Forge = {
        init: function(theme) {},
        update: function(p, stage) {
            progress = p;
            stageName = stage || stageName;
        },
        reveal: function() {
            if (window.ForgeBridge && window.ForgeBridge.onRevealFinished) {
                window.ForgeBridge.onRevealFinished();
            }
        },
        dispose: function() {}
    };

    if (window.ForgeBridge && window.ForgeBridge.onReady) {
        window.ForgeBridge.onReady();
    }
})();
