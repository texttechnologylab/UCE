/**
 * Gets the primaryColor from the css that is being applied by uceConfig.json
 * @returns {{primaryColor: string}}
 */
function getCustomUCEColors() {
    const langSelect = document.querySelector('#prime-color-container');
    const styles = window.getComputedStyle(langSelect);
    return {primaryColor: styles.color};
}

function rgbToHex(rgb) {
    const result = rgb.match(/\d+/g);
    if (!result || result.length < 3) return '#000000';
    const [r, g, b] = result.map(x => parseInt(x).toString(16).padStart(2, '0'));
    return `#${r}${g}${b}`;
}

function rgbaToHex(rgba) {
    if (!rgba) return '#000000';
    const rgbaMatch = rgba.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*[\d.]+)?\)/);
    if (!rgbaMatch) return '#000000';
    const r = parseInt(rgbaMatch[1]);
    const g = parseInt(rgbaMatch[2]);
    const b = parseInt(rgbaMatch[3]);
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}

function hexToRgb(hex) {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

function convertToRGBA(hex, alpha = 0.6) {
    hex = hex.replace(/^#/, '');
    if (hex.length === 3) hex = hex.split('').map(c => c + c).join('');
    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function generateColorPalette(baseHex, count, alpha = 0.6) {
    const baseHSL = hexToHSL(baseHex);
    const palette = [];

    for (let i = 0; i < count; i++) {
        const hue = (baseHSL.h + (i * 30)) % 360;
        const lightness = Math.max(40, Math.min(70, baseHSL.l + (i - count / 2) * 5));
        palette.push(`hsla(${hue}, ${baseHSL.s}%, ${lightness}%, ${alpha})`);
    }

    return palette;
}

function hexToHSL(hex) {
    hex = hex.replace(/^#/, '');
    if (hex.length === 3) hex = hex.split('').map(ch => ch + ch).join('');
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;

    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;

    if (max === min) {
        h = s = 0;
    } else {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = ((g - b) / d + (g < b ? 6 : 0)); break;
            case g: h = ((b - r) / d + 2); break;
            case b: h = ((r - g) / d + 4); break;
        }
        h *= 60;
    }

    return {
        h: Math.round(h),
        s: Math.round(s * 100),
        l: Math.round(l * 100)
    };
}

function isElementInViewport($element) {
    if ($element.length === 0) return false;

    const elementTop = $element.offset().top;
    const elementBottom = elementTop + $element.outerHeight();
    const viewportTop = $(window).scrollTop();
    const viewportBottom = viewportTop + $(window).height();

    return (elementTop >= viewportTop && elementTop <= viewportBottom) ||
        (elementBottom >= viewportTop && elementBottom <= viewportBottom) ||
        (elementTop <= viewportTop && elementBottom >= viewportBottom);
}

function makeDraggable(el, handleSelector = null) {
    const handle = handleSelector ? el.querySelector(handleSelector) : el;
    if (!handle) return;

    handle.style.cursor = 'move';

    let offsetX = 0, offsetY = 0;

    handle.addEventListener('pointerdown', (evt) => {
        if (evt.button !== 0 && evt.pointerType === 'mouse') return;

        offsetX = evt.clientX - el.offsetLeft;
        offsetY = evt.clientY - el.offsetTop;

        el.setPointerCapture(evt.pointerId);
        el.style.userSelect = 'none';

        el.addEventListener('pointermove', onMove);
        el.addEventListener('pointerup', endDrag);
        el.addEventListener('pointercancel', endDrag);
    });

    function onMove(evt) {
        el.style.left = (evt.clientX - offsetX) + 'px';
        el.style.top = (evt.clientY - offsetY) + 'px';
        el.style.right = 'auto';
        el.style.bottom = 'auto';
        el.style.transform = 'none';
    }

    function endDrag(evt) {
        el.releasePointerCapture(evt.pointerId);
        el.style.userSelect = '';
        el.removeEventListener('pointermove', onMove);
        el.removeEventListener('pointerup', endDrag);
        el.removeEventListener('pointercancel', endDrag);
    }
}

