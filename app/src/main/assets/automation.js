/**
 * WaterFox Smart Automation Content Script
 * Detects form fields, handles React/Vue/Angular frameworks, shadow DOMs,
 * same-origin iframes, and provides undo capability.
 */
(function() {
    if (window.__waterfox_automation_initialized__) return;
    window.__waterfox_automation_initialized__ = true;

    // Validate origin: frame origin must match top-level window
    function isSameOriginSafe() {
        try {
            if (window === window.top) return true;
            return window.location.origin === window.top.location.origin;
        } catch (e) {
            return false;
        }
    }

    if (!isSameOriginSafe()) {
        return;
    }

    function normalizeString(str) {
        if (!str) return '';
        return str.toLowerCase()
            .replace(/[_\-]+/g, ' ')
            .replace(/([a-z])([A-Z])/g, '$1 $2')
            .replace(/[^a-z0-9 ]/g, '')
            .trim();
    }

    const FIELD_SYNONYMS = {
        first_name: ['first name', 'firstname', 'fname', 'given name', 'forename', 'first'],
        last_name: ['last name', 'lastname', 'lname', 'surname', 'family name', 'last'],
        full_name: ['full name', 'fullname', 'your name', 'name', 'author', 'sender', 'customer name', 'contact name'],
        middle_name: ['middle name', 'middlename', 'mname', 'middle initial'],
        username: ['username', 'user name', 'userid', 'user id', 'login', 'handle', 'nickname', 'user', 'account', 'screen name'],
        email: ['email', 'e mail', 'mail', 'email address', 'e mail address', 'user email', 'login email'],
        confirm_email: ['confirm email', 'verify email', 're enter email', 'repeat email', 'email confirmation'],
        password: ['password', 'pass', 'pwd', 'user password', 'login password', 'account password'],
        confirm_password: ['confirm password', 'verify password', 're enter password', 'repeat password', 'password confirmation'],
        phone: ['phone', 'mobile', 'telephone', 'tel', 'cell', 'cellphone', 'phone number', 'mobile number', 'contact number', 'phone no'],
        country_code: ['country code', 'dial code', 'calling code', 'phone code', 'area code', 'prefix'],
        date_of_birth: ['date of birth', 'dob', 'birthday', 'birth date', 'date of birth dd mm yyyy'],
        birth_day: ['birth day', 'day of birth', 'bday day', 'day'],
        birth_month: ['birth month', 'month of birth', 'bday month', 'month'],
        birth_year: ['birth year', 'year of birth', 'bday year', 'year'],
        age: ['age', 'years old', 'your age'],
        gender: ['gender', 'sex'],
        title: ['title', 'prefix', 'salutation', 'mr', 'ms', 'mrs', 'dr'],
        company: ['company', 'organization', 'organisation', 'business', 'employer', 'workplace', 'firm', 'agency'],
        job_title: ['job title', 'job', 'position', 'role', 'occupation', 'profession'],
        website: ['website', 'url', 'site', 'web page', 'web address', 'homepage', 'blog'],
        address_line_1: ['address line 1', 'address 1', 'street address', 'street', 'address', 'shipping address', 'billing address'],
        address_line_2: ['address line 2', 'address 2', 'apt', 'suite', 'unit', 'building', 'floor', 'apartment'],
        city: ['city', 'town', 'suburb', 'locality', 'municipality'],
        state: ['state', 'province', 'region', 'county', 'district', 'state province'],
        zip_code: ['zip code', 'zip', 'postal code', 'postcode', 'pin code', 'pincode'],
        country: ['country', 'nation'],
        subject: ['subject', 'topic', 'title of message', 'heading', 'reason'],
        message: ['message', 'comment', 'comments', 'feedback', 'inquiry', 'enquiry', 'notes', 'description', 'body', 'details'],
        rating: ['rating', 'score', 'stars'],
        security_question: ['security question', 'security question 1', 'secret question', 'recovery question'],
        security_answer: ['security answer', 'security answer 1', 'secret answer', 'recovery answer', 'answer']
    };

    function findNearbyLabel(el) {
        if (!el) return '';
        let labelText = '';
        if (el.id) {
            try {
                const labelElem = document.querySelector('label[for="' + CSS.escape(el.id) + '"]');
                if (labelElem && labelElem.textContent) {
                    labelText = labelElem.textContent.trim();
                }
            } catch (e) {}
        }
        if (!labelText) {
            const parentLabel = el.closest('label');
            if (parentLabel && parentLabel.textContent) {
                labelText = parentLabel.textContent.trim();
            }
        }
        if (!labelText) {
            labelText = el.getAttribute('aria-label') || el.getAttribute('placeholder') || el.getAttribute('title') || '';
        }
        if (!labelText && el.parentElement) {
            // Check previous sibling or parent header
            const prev = el.previousElementSibling;
            if (prev && (prev.tagName === 'LABEL' || prev.tagName === 'SPAN' || prev.tagName === 'P')) {
                labelText = prev.textContent.trim();
            }
        }
        return labelText;
    }

    function isHoneypotOrSearch(el) {
        const type = (el.getAttribute('type') || '').toLowerCase();
        if (type === 'search') return true;

        const role = (el.getAttribute('role') || '').toLowerCase();
        if (role === 'search') return true;

        const name = (el.getAttribute('name') || '').toLowerCase();
        const id = (el.getAttribute('id') || '').toLowerCase();

        // Standard search box names
        if (name === 'q' || name === 'query' || name === 's' || name === 'search' || name === 'search_query' || name === 'k' || name === 'keyword') {
            return true;
        }

        // Honeypot indicators
        if (name.includes('honeypot') || id.includes('honeypot') || name.includes('hp_') || id.includes('hp_')) {
            return true;
        }
        if (el.getAttribute('tabindex') === '-1' && el.getAttribute('aria-hidden') === 'true') {
            return true;
        }

        // Check off-screen or invisible honeypot styles
        try {
            const style = window.getComputedStyle(el);
            if (style) {
                if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return true;
                const rect = el.getBoundingClientRect();
                if (rect.top < -500 || rect.left < -500) return true;
            }
        } catch (e) {}

        return false;
    }

    function classifyElement(el) {
        const tag = el.tagName.toLowerCase();
        const type = (el.getAttribute('type') || (tag === 'textarea' ? 'textarea' : 'text')).toLowerCase();
        const name = normalizeString(el.getAttribute('name') || '');
        const id = normalizeString(el.getAttribute('id') || '');
        const placeholder = normalizeString(el.getAttribute('placeholder') || '');
        const ariaLabel = normalizeString(el.getAttribute('aria-label') || '');
        const autocomplete = (el.getAttribute('autocomplete') || '').toLowerCase();
        const labelText = normalizeString(findNearbyLabel(el));

        const combinedText = [labelText, name, id, placeholder, ariaLabel, autocomplete].join(' ');

        // Autocomplete standard hints
        if (autocomplete.includes('given-name')) return 'first_name';
        if (autocomplete.includes('family-name')) return 'last_name';
        if (autocomplete.includes('name') && !autocomplete.includes('username')) return 'full_name';
        if (autocomplete.includes('username')) return 'username';
        if (autocomplete.includes('email')) return 'email';
        if (autocomplete.includes('new-password')) return 'password';
        if (autocomplete.includes('current-password')) return 'password';
        if (autocomplete.includes('tel')) return 'phone';
        if (autocomplete.includes('address-line1')) return 'address_line_1';
        if (autocomplete.includes('address-line2')) return 'address_line_2';
        if (autocomplete.includes('address-level2')) return 'city';
        if (autocomplete.includes('address-level1')) return 'state';
        if (autocomplete.includes('postal-code')) return 'zip_code';
        if (autocomplete.includes('country')) return 'country';

        // Check password confirmations specifically
        if (type === 'password') {
            for (let syn of FIELD_SYNONYMS.confirm_password) {
                if (combinedText.includes(syn)) return 'confirm_password';
            }
            return 'password';
        }

        // Check email confirmations specifically
        for (let syn of FIELD_SYNONYMS.confirm_email) {
            if (combinedText.includes(syn)) return 'confirm_email';
        }

        // Check all synonym mappings
        for (let key in FIELD_SYNONYMS) {
            const synonyms = FIELD_SYNONYMS[key];
            for (let syn of synonyms) {
                if (combinedText.includes(syn)) {
                    return key;
                }
            }
        }

        if (type === 'email') return 'email';
        if (type === 'tel') return 'phone';
        if (tag === 'textarea') return 'message';

        return name || id || 'custom';
    }

    function getAllCandidateInputs(root) {
        let results = [];
        if (!root) return results;

        const selector = 'input, textarea, select, [contenteditable="true"]';
        try {
            const nodes = root.querySelectorAll(selector);
            nodes.forEach(function(node) {
                // Ignore disabled, readonly, hidden types, honeypots, search boxes
                if (node.disabled || node.readOnly) return;
                const type = (node.getAttribute('type') || '').toLowerCase();
                if (['hidden', 'submit', 'button', 'reset', 'image'].includes(type)) return;
                if (isHoneypotOrSearch(node)) return;

                results.push(node);
            });
        } catch (e) {}

        // Traverse open shadow roots
        try {
            const allElements = root.querySelectorAll('*');
            allElements.forEach(function(el) {
                if (el.shadowRoot) {
                    const shadowCandidates = getAllCandidateInputs(el.shadowRoot);
                    results = results.concat(shadowCandidates);
                }
            });
        } catch (e) {}

        // Traverse same-origin iframes
        try {
            const iframes = root.querySelectorAll('iframe');
            iframes.forEach(function(iframe) {
                try {
                    if (iframe.contentDocument) {
                        const iframeCandidates = getAllCandidateInputs(iframe.contentDocument);
                        results = results.concat(iframeCandidates);
                    }
                } catch (e) {}
            });
        } catch (e) {}

        return results;
    }

    function scanPageForForms() {
        const inputs = getAllCandidateInputs(document);
        let eligibleFields = [];

        inputs.forEach(function(input, index) {
            const rect = input.getBoundingClientRect();
            if (rect.width > 0 || rect.height > 0 || input.offsetWidth > 0) {
                if (!input.dataset.waterfoxId) {
                    input.dataset.waterfoxId = 'wf_field_' + index + '_' + Date.now();
                }
                const fieldType = classifyElement(input);
                eligibleFields.push({
                    id: input.dataset.waterfoxId,
                    fieldType: fieldType
                });
            }
        });

        if (eligibleFields.length > 0 && window.WaterFoxAutomation && window.WaterFoxAutomation.onFormsDetected) {
            try {
                window.WaterFoxAutomation.onFormsDetected(JSON.stringify({
                    url: window.location.href,
                    count: eligibleFields.length
                }));
            } catch (e) {}
        }
    }

    // Set value using native prototype setter for React/Vue/Angular
    function setNativeValue(el, val) {
        const isTextArea = el.tagName.toLowerCase() === 'textarea';
        const proto = isTextArea ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
        const descriptor = Object.getOwnPropertyDescriptor(proto, 'value');

        if (descriptor && descriptor.set) {
            descriptor.set.call(el, val);
        } else {
            el.value = val;
        }

        // Trigger input, change, blur events
        try {
            el.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
            el.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }));
            el.dispatchEvent(new Event('blur', { bubbles: true }));
        } catch (e) {}
    }

    // Stores previous state for undo capability
    window.__waterfox_undo_state = [];

    window.__waterfox_undo_fill = function() {
        if (!window.__waterfox_undo_state || window.__waterfox_undo_state.length === 0) return false;
        window.__waterfox_undo_state.forEach(function(item) {
            try {
                const el = item.element;
                if (!el || !document.contains(el)) return;
                if (el.isContentEditable) {
                    el.textContent = item.prevValue;
                } else if (el.type === 'checkbox' || el.type === 'radio') {
                    el.checked = item.prevChecked;
                } else if (el.tagName.toLowerCase() === 'select') {
                    el.selectedIndex = item.prevIndex;
                } else {
                    setNativeValue(el, item.prevValue);
                }
            } catch (e) {}
        });
        window.__waterfox_undo_state = [];
        return true;
    };

    // Public Fill Function called from Android
    window.__waterfox_fill_fields = function(fieldValuesJson, highlight, autoSubmit) {
        let valuesMap = {};
        try {
            valuesMap = JSON.parse(fieldValuesJson);
        } catch (e) {
            return JSON.stringify({ success: false, filledCount: 0, totalEligible: 0 });
        }

        const inputs = getAllCandidateInputs(document);
        let filledCount = 0;
        let submitButton = null;
        window.__waterfox_undo_state = [];

        inputs.forEach(function(input) {
            const fieldType = classifyElement(input);
            const val = valuesMap[fieldType] || valuesMap[input.name] || valuesMap[input.id];

            if (val !== undefined && val !== null && val !== '') {
                const tag = input.tagName.toLowerCase();

                // Save undo record
                const undoRecord = {
                    element: input,
                    prevValue: input.value || '',
                    prevChecked: input.checked || false,
                    prevIndex: (tag === 'select') ? input.selectedIndex : 0
                };
                window.__waterfox_undo_state.push(undoRecord);

                input.focus();

                if (input.isContentEditable) {
                    input.textContent = val;
                    try {
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                    } catch (e) {}
                } else if (tag === 'select') {
                    let matched = false;
                    const strVal = String(val).toLowerCase();
                    for (let i = 0; i < input.options.length; i++) {
                        const opt = input.options[i];
                        if (opt.value.toLowerCase() === strVal || opt.text.toLowerCase().includes(strVal)) {
                            input.selectedIndex = i;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched && input.options.length > 0) {
                        input.value = val;
                    }
                    try {
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    } catch (e) {}
                } else if (input.type === 'checkbox') {
                    const boolVal = (val === true || val === 'true' || val === '1' || val === 'yes');
                    input.checked = boolVal;
                    try {
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    } catch (e) {}
                } else if (input.type === 'radio') {
                    if (input.value.toLowerCase() === String(val).toLowerCase()) {
                        input.checked = true;
                        try {
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                        } catch (e) {}
                    }
                } else if (input.type === 'date') {
                    // Normalize date format to yyyy-MM-dd
                    let dateStr = String(val);
                    if (dateStr.includes('/')) {
                        const parts = dateStr.split('/');
                        if (parts.length === 3) {
                            if (parts[2].length === 4) dateStr = parts[2] + '-' + parts[0].padStart(2, '0') + '-' + parts[1].padStart(2, '0');
                        }
                    }
                    setNativeValue(input, dateStr);
                } else {
                    setNativeValue(input, val);
                }

                if (highlight) {
                    const origTransition = input.style.transition;
                    const origOutline = input.style.outline;
                    const origBg = input.style.backgroundColor;
                    input.style.transition = 'all 0.3s ease';
                    input.style.outline = '2.5px solid #DE5833';
                    input.style.backgroundColor = 'rgba(222, 88, 51, 0.12)';

                    setTimeout(function() {
                        input.style.outline = origOutline;
                        input.style.backgroundColor = origBg;
                        input.style.transition = origTransition;
                    }, 2200);
                }

                filledCount++;

                if (autoSubmit && !submitButton && input.form) {
                    submitButton = input.form.querySelector('button[type="submit"], input[type="submit"]');
                }
            }
        });

        // Re-apply once if form re-renders or resets within 350ms (common in React/Vue)
        setTimeout(function() {
            inputs.forEach(function(input) {
                const fieldType = classifyElement(input);
                const val = valuesMap[fieldType] || valuesMap[input.name] || valuesMap[input.id];
                if (val !== undefined && val !== null && val !== '') {
                    if (!input.isContentEditable && input.type !== 'checkbox' && input.type !== 'radio' && input.tagName.toLowerCase() !== 'select') {
                        if (input.value === '') {
                            setNativeValue(input, val);
                        }
                    }
                }
            });
        }, 350);

        if (autoSubmit && filledCount > 0) {
            setTimeout(function() {
                if (submitButton) {
                    submitButton.click();
                } else {
                    const activeForm = document.querySelector('form');
                    if (activeForm) {
                        try { activeForm.submit(); } catch (e) {}
                    }
                }
                if (window.WaterFoxAutomation && window.WaterFoxAutomation.onFormSubmitted) {
                    try { window.WaterFoxAutomation.onFormSubmitted(); } catch (e) {}
                }
            }, 600);
        }

        return JSON.stringify({
            success: true,
            filledCount: filledCount,
            totalEligible: inputs.length
        });
    };

    // Debounced scan observer
    let scanTimer = null;
    function scheduleScan() {
        if (scanTimer) clearTimeout(scanTimer);
        scanTimer = setTimeout(scanPageForForms, 400);
    }

    const observer = new MutationObserver(function() {
        scheduleScan();
    });

    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true });
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            }
            scheduleScan();
        });
    }

    scheduleScan();
    setTimeout(scheduleScan, 1000);
    setTimeout(scheduleScan, 3000);
})();
