"""Build editable draw.io authentication package diagrams and supporting previews.

Only docs/architecture is written. Previews are rendered from the saved mxGraph
XML with Pillow; they are not a native diagrams.net rendering or runtime proof.
"""
from __future__ import annotations

import hashlib
import html
import json
import math
import re
from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent
ROOT = OUT.parent.parent
JAVA = ROOT / 'src/main/java/com/group2/rms'
W, H = 1740, 1610
BG, FILL, GROUP = '#7ECBE9', '#9AD8EF', '#86CEE9'
INK = '#142C38'
RENDER_ISSUES = []
PROOFS = {
    'E01': 'auth/controller/RegistrationController.java:3,20-23; auth/controller/PasswordRecoveryController.java:4,18-21',
    'E02': 'auth/controller/AuthController.java:9-11; auth/controller/RegistrationController.java:26-72; auth/controller/PasswordRecoveryController.java:25-125',
    'E03': 'auth/service/RegistrationVerificationService.java:7,30,36-40,50,104-108',
    'E04': 'auth/service/PasswordResetService.java:6-7,31-44,58,85,107,115-116',
    'E05': 'user/service/AccountManagementService.java:27-28,94-130',
    'E06': 'user/service/AccountManagementService.java:30,128-130,224-225',
    'E07': 'user/repository/UserRepository.java:3,24; user/repository/RoleRepository.java:3,14',
    'E08': 'candidate/repository/CandidateRepository.java:3,16',
    'E09': 'candidate/entity/Candidate.java:4,29-31',
    'E10': 'core/config/SecurityConfig.java:4-6,50-56,174-175',
    'E11': 'core/security/DatabaseUserDetailsService.java:3-4,17-20,32-33; core/security/AccountSessionGuardFilter.java:3-4,19-23,34',
    'E12': 'core/config/SecurityConfig.java:3,50-56,174',
    'E13': 'core/config/SecurityConfig.java:11-25,45-46,162-175',
    'E14': 'core/security/DatabaseUserDetailsService.java:5-7,15,51-55; core/security/AccountSessionGuardFilter.java:9-13,17,30-52',
    'E15': 'auth/service/RegistrationVerificationService.java:36-40; auth/service/PasswordRecoveryService.java:39-49; auth/service/PasswordResetService.java:11,31-44',
    'E16': 'auth/service/RegistrationVerificationService.java:155-173; auth/service/PasswordRecoveryService.java:91,136,161,187-210',
    'E17': 'auth/service/RegistrationEmailSender.java:12-13,22-25,33-45; auth/service/PasswordResetEmailSender.java:7-8,28-33,47-62',
    'E18': 'Spring Mail adapter + src/main/resources/application.properties:50-55; configuration evidence, not a live SMTP test',
    'E19': 'user/repository/UserRepository.java:24; user/repository/RoleRepository.java:14; candidate/repository/CandidateRepository.java:16',
    'E20': 'pom.xml:49,62; src/main/resources/application.properties:15; configured JPA/JDBC integration, not direct repository SQL calls',
    'E21': 'auth/controller/RegistrationController.java:4-5; auth/controller/PasswordRecoveryController.java:3,5-6',
    'E22': 'auth/service/RegistrationVerificationService.java:3-10; auth/service/PasswordRecoveryService.java:14-17; auth/service/PasswordResetService.java:2-5',
    'E23': 'auth/dto/RegisterAccountRequest.java:8,12; auth/dto/ResetPasswordRequest.java:3,11',
    'E24': 'auth/exception/RegistrationFlowException.java:3,5; auth/exception/PasswordRecoveryFlowException.java:3,5; auth/exception/PasswordRecoveryUnavailableException.java:3,5; auth/exception/PasswordResetDeliveryException.java:3,5',
    'E25': 'core/exception/GlobalExceptionHandler.java:9,11-13,46,144-148,243',
    'E26': 'core/exception/GlobalExceptionHandler.java:5-10,51-60,151,254-263',
    'E27': 'user/service/AccountManagementService.java:200-213',
    'E28': 'user/exception/AccountFieldException.java:3,6',
    'E29': 'user/entity/User.java:3,20; candidate/entity/Candidate.java:3,22',
    'E30': 'auth/NavigationAdvice.java:3,7-9',
    'E31': 'user/service/AccountManagementService.java:107,117-126,224-225',
    'E32': 'auth/service/RegistrationEmailSender.java:51-54; auth/service/PasswordResetEmailSender.java:68-71',
    'E33': 'auth/service/RegistrationVerificationService.java:8,51-53,109-112',
}


class Page:
    def __init__(self, key, title, subtitle, note):
        self.key, self.title, self.subtitle, self.note = key, title, subtitle, note
        self.nodes, self.edges = [], []

    def node(self, key, title, lines, x, y, w, h, parent=None, kind='package', actual=None):
        if kind == 'external':
            h = max(h, 100)
        self.nodes.append(dict(id=key, title=title, lines=lines, x=x, y=y, w=w, h=h,
                               parent=parent, kind=kind, actual=actual))
        return key

    def group(self, key, title, x, y, w, h):
        return self.node(key, title + '  «logical»', [], x, y, w, h, kind='group')

    def edge(self, source, target, proof, label, exit=(.5, 1), entry=(.5, 0), points=(), kind='dependency'):
        assert proof in PROOFS
        self.edges.append(dict(id=f'{self.key}_edge_{len(self.edges)+1}', source=source, target=target,
                               proof=proof, label=label, exit=exit, entry=entry, points=points, kind=kind))


def common(p, presentation_h=220, business_y=465, business_h=390):
    p.group('presentation', 'Presentation Layer', 245, 135, 940, presentation_h)
    p.group('business', 'Business Layer', 245, business_y, 940, business_h)
    p.group('data', 'Data Layer', 245, 970, 940, 200)
    p.group('cross', 'Cross-Cutting / Frameworks', 1250, 135, 420, 1035)
    p.node('actors', 'Users / Browser', ['«external context»', 'Candidates', 'Internal users', 'HTTP forms + session'], 25, 195, 185, 200, kind='context')
    p.node('controller', 'auth.controller', ['Auth / Registration / Recovery', 'MVC controllers (scope slice)'], 275, 195, 350, 110, 'presentation', actual='auth.controller')
    p.node('views', 'templates/auth/', ['«resource folder»', 'Forms + email/ templates'], 805, 195, 350, 110, 'presentation', kind='resource')
    p.edge('controller', 'views', 'E02', 'view names', (1, .5), (0, .5))


def persistence(p, candidate=False):
    p.node('jpa', 'Spring Data JPA / JDBC', ['«framework group»', 'JpaRepository + SQL Server driver'], 495, 1260, 520, 110, kind='framework')
    p.node('db', 'SQL Server Database', ['«external system»', 'Configured data source'], 495, 1450, 520, 90, kind='external')
    for key, x in [('users', 450), ('candidates', 980)] if candidate else [('users', 980)]:
        p.edge(key, 'jpa', 'E19', 'JpaRepository', (.5, 1), (.25 if key == 'users' else .8, 0),
               [(x, 1210), (625 if key == 'users' else 911, 1210)])
    p.edge('jpa', 'db', 'E20', '«configured integration»')


def overview():
    p = Page('overview', 'RMS Authentication — Layered Package Diagram',
             'Authentication first • Spring Boot monolith • Java names relative to com.group2.rms',
             'Logical layers group real packages. This is a dependency view, not a request sequence. Contracts and the exception cycle are expanded on detailed page 04.')
    common(p, presentation_h=300)
    p.node('advice', 'auth', ['NavigationAdvice (root package)'], 805, 345, 350, 90, 'presentation', actual='auth')
    p.node('service', 'auth.service', ['RegistrationVerificationService', 'PasswordRecoveryService / PasswordResetService', 'RegistrationEmailSender / PasswordResetEmailSender'], 275, 525, 420, 165, 'business', actual='auth.service')
    p.node('accounts', 'user.service', ['AccountManagementService', 'Candidate registration methods only'], 805, 525, 350, 145, 'business', actual='user.service')
    p.node('user_entity', 'user.entity', ['User / Role', 'Account / authority data'], 275, 725, 350, 100, 'business', actual='user.entity')
    p.node('candidate_entity', 'candidate.entity', ['Candidate → linked User'], 805, 725, 350, 100, 'business', actual='candidate.entity')
    p.node('users', 'user.repository', ['UserRepository / RoleRepository'], 275, 1030, 350, 110, 'data', actual='user.repository')
    p.node('candidates', 'candidate.repository', ['CandidateRepository'], 805, 1030, 350, 110, 'data', actual='candidate.repository')
    p.node('config', 'core.config', ['SecurityConfig', 'Filter chain / BCrypt bean'], 1280, 195, 360, 110, 'cross', actual='core.config')
    p.node('security', 'core.security', ['DatabaseUserDetailsService', 'AccountSessionGuardFilter / RoleAuthorities'], 1280, 390, 360, 130, 'cross', actual='core.security')
    p.node('contracts', 'Contracts & Validation', ['«logical group»', 'auth.dto / core.validation'], 1280, 610, 360, 110, 'cross', kind='logical')
    p.node('exceptions', 'Exception Handling', ['«logical group»', 'auth.exception ↔ core.exception', 'user.exception / core.base'], 1280, 820, 360, 140, 'cross', kind='logical')
    p.node('mail', 'Spring Mail', ['«framework group»', 'JavaMailSender → SMTP adapter'], 1280, 1030, 360, 110, 'cross', kind='framework')
    p.node('smtp', 'SMTP Server', ['«external system»', 'Configured email delivery'], 1280, 1450, 360, 90, kind='external')
    p.edge('controller', 'service', 'E01', 'injects service', (.5, 1), (.42, 0))
    p.edge('service', 'accounts', 'E03', 'candidate registration', (1, .35), (0, .35))
    p.edge('service', 'users', 'E04', 'password reset persistence', (1, .75), (.7, 0), [(735, 634), (735, 940), (520, 940)])
    p.edge('accounts', 'users', 'E05', 'account / role access', (.15, 1), (.25, 0), [(857, 695), (700, 695), (700, 915), (362.5, 915)])
    p.edge('accounts', 'candidates', 'E06', 'creates candidate profile', (1, .6), (1, .5), [(1215, 612), (1215, 1085)])
    p.edge('accounts', 'candidate_entity', 'E31', 'constructs', (.5, 1), (.5, 0))
    p.edge('users', 'user_entity', 'E07', 'entity type', (.5, 0), (.5, 1))
    p.edge('candidates', 'candidate_entity', 'E08', 'entity type', (.5, 0), (.5, 1))
    p.edge('candidate_entity', 'user_entity', 'E09', 'account type', (0, .5), (1, .5))
    p.edge('config', 'security', 'E10', 'wires')
    p.edge('security', 'users', 'E11', 'reloads account', (0, .5), (.85, 0), [(1230, 455), (1230, 895), (572.5, 895)])
    p.edge('service', 'mail', 'E17', 'uses JavaMailSender', (1, .95), (0, .5), [(755, 662.75), (755, 945), (1235, 945), (1235, 1085)])
    p.edge('mail', 'smtp', 'E18', '«configured SMTP integration»', kind='integration')
    persistence(p, True)
    return p


def login():
    p = Page('login', '01 — Login, Logout & Session Guard',
             'Package dependencies • Spring Security processes POST /login and /logout • GET /login returns the view',
             'AuthController does not authenticate credentials. SecurityConfig wires DaoAuthenticationProvider, BCrypt and AccountSessionGuardFilter; security packages depend on repository/entity types.')
    common(p, presentation_h=300, business_y=535, business_h=200)
    next(n for n in p.nodes if n['id'] == 'controller')['lines'] = ['AuthController', 'GET /login → auth/login']
    p.node('advice', 'auth', ['NavigationAdvice'], 275, 345, 350, 90, 'presentation', actual='auth')
    p.node('user_entity', 'user.entity', ['User / Role'], 605, 595, 390, 100, 'business', actual='user.entity')
    p.node('users', 'user.repository', ['UserRepository', 'Username / email lookup'], 805, 1030, 350, 110, 'data', actual='user.repository')
    p.node('config', 'core.config', ['SecurityConfig', 'formLogin / logout / request cache', 'BCryptPasswordEncoder bean'], 1280, 195, 360, 150, 'cross', actual='core.config')
    p.node('security', 'core.security', ['DatabaseUserDetailsService', 'AccountSessionGuardFilter', 'RoleAuthorities', 'UserDetailsService implementation'], 1280, 455, 360, 180, 'cross', actual='core.security')
    p.node('framework_security', 'Spring Security', ['«framework group»', 'org.springframework.security.*', 'DaoAuthenticationProvider', 'SecurityFilterChain / PasswordEncoder'], 1280, 800, 360, 205, 'cross', kind='framework')
    p.edge('config', 'security', 'E10', 'injects / constructs')
    p.edge('config', 'users', 'E12', 'injects repository', (1, .4), (1, .8), [(1700, 255), (1700, 1195), (1210, 1195), (1210, 1118)])
    p.edge('config', 'framework_security', 'E13', 'configures filter chain', (1, .8), (1, .2), [(1675, 315), (1675, 841)])
    p.edge('security', 'users', 'E11', 'injects / queries', (0, .5), (.5, 0), [(1225, 545), (1225, 935), (980, 935)])
    p.edge('security', 'user_entity', 'E11', 'uses domain type', (0, .2), (1, .5), [(1205, 491), (1205, 645)])
    p.edge('security', 'framework_security', 'E14', 'implements / extends')
    p.edge('users', 'user_entity', 'E07', 'entity type', (.25, 0), (.75, 1), [(892.5, 850), (897.5, 850)])
    p.edge('advice', 'framework_security', 'E30', 'Authentication API', (1, .5), (0, .75), [(1190, 380), (1190, 953.75)])
    persistence(p)
    return p


def otp_page(recovery=False):
    key = 'recovery' if recovery else 'registration'
    p = Page(key, '03 — Password Recovery OTP' if recovery else '02 — Candidate Registration OTP',
             'Package dependencies • Internal service calls remain inside auth.service • DTO/exception dependencies are on page 04',
             'OTP state is stored in HttpSession. PasswordResetService is active. No JWT, token repository or recovery entity is introduced.' if recovery else
             'Registration checks identifiers, verifies the emailed OTP, then delegates User + Candidate creation to AccountManagementService. Department administration is outside this path.')
    common(p)
    next(n for n in p.nodes if n['id'] == 'controller')['lines'] = ['PasswordRecoveryController', '/forgot-password / /reset-password/**'] if recovery else ['RegistrationController', '/register / /register/otp/**']
    p.node('service', 'auth.service', ['PasswordRecoveryService', 'PasswordResetService', 'PasswordResetEmailSender'] if recovery else ['RegistrationVerificationService', 'RegistrationEmailSender'], 275, 525, 550 if recovery else 350, 145, 'business', actual='auth.service')
    p.node('user_entity', 'user.entity', ['User / Role'] if not recovery else ['User'], 805 if recovery else 275, 725, 350, 100, 'business', actual='user.entity')
    p.node('users', 'user.repository', ['UserRepository'] if recovery else ['UserRepository / RoleRepository'], 805 if recovery else 275, 1030, 350, 110, 'data', actual='user.repository')
    p.node('framework_security', 'Spring Security', ['«framework group»', 'PasswordEncoder'], 1280, 405, 360, 130, 'cross', kind='framework')
    p.node('session', 'Jakarta Servlet', ['«framework package»', 'jakarta.servlet.http.HttpSession', 'OTP / pending state'], 1280, 625, 360, 120, 'cross', kind='framework')
    p.node('mail', 'Spring Mail', ['«framework group»', 'JavaMailSender / MimeMessageHelper'], 1280, 825, 360, 110, 'cross', kind='framework')
    p.node('smtp', 'SMTP Server', ['«external system»', 'Configured OTP email delivery'], 1280, 1450, 360, 90, kind='external')
    p.node('contracts_note', 'Shared contracts', ['«navigation note»', 'See page 04: DTO validation', 'and auth/core exception cycle'], 1280, 195, 360, 120, 'cross', kind='note')
    p.edge('controller', 'service', 'E01', 'injects', (.5, 1), (.32 if recovery else .5, 0))
    p.edge('service', 'framework_security', 'E15', 'PasswordEncoder', (.05 if recovery else .75, 0), (0, .5), [(302.5 if recovery else 537.5, 430), (1225, 430), (1225, 470)])
    p.edge('service', 'session', 'E16', 'session state API', (1, .85), (0, .5), [(1190 if recovery else 750, 648.25), (1190 if recovery else 750, 685), (1240, 685), (1240, 680)])
    p.edge('service', 'mail', 'E17', 'mail API', (1, .65), (0, .5), [(1210 if recovery else 730, 619.25), (1210 if recovery else 730, 900), (1255, 900), (1255, 880)])
    p.edge('service', 'views', 'E32', 'loads email resources', (.2 if recovery else .9, 0), (.6, 1), [(385 if recovery else 590, 390), (1015, 390)])
    p.edge('mail', 'smtp', 'E18', '«configured SMTP integration»', kind='integration')
    if recovery:
        p.edge('service', 'users', 'E04', 'queries / updates', (1, .2), (1, .5), [(1195, 554), (1195, 1085)])
        p.edge('service', 'user_entity', 'E04', 'uses User type', (.5, 1), (0, .5), [(550, 775)])
    else:
        p.node('accounts', 'user.service', ['AccountManagementService', 'Registration methods only'], 805, 525, 350, 145, 'business', actual='user.service')
        p.node('candidate_entity', 'candidate.entity', ['Candidate'], 805, 725, 350, 100, 'business', actual='candidate.entity')
        p.node('candidates', 'candidate.repository', ['CandidateRepository'], 805, 1030, 350, 110, 'data', actual='candidate.repository')
        p.edge('service', 'accounts', 'E03', 'checks / creates', (1, .3), (0, .3))
        p.edge('accounts', 'users', 'E05', 'User / Role access', (.15, 1), (.2, 0), [(857.5, 700), (700, 700), (700, 945), (345, 945)])
        p.edge('accounts', 'candidates', 'E06', 'persists profile', (1, .6), (1, .5), [(1215, 612), (1215, 1085)])
        p.edge('accounts', 'candidate_entity', 'E31', 'constructs', (.5, 1), (.5, 0))
        p.edge('accounts', 'user_entity', 'E31', 'constructs', (0, .7), (.5, 0), [(780, 626.5), (780, 710), (450, 710)])
        p.edge('candidates', 'candidate_entity', 'E08', 'entity type', (.5, 0), (.5, 1))
        p.edge('candidate_entity', 'user_entity', 'E09', 'User account type', (0, .5), (1, .5))
    p.edge('users', 'user_entity', 'E07', 'entity type', (.5, 0), (.5, 1))
    persistence(p, not recovery)
    return p


def contracts():
    p = Page('contracts', '04 — Contracts, Validation & Exception Dependencies',
             'Actual Java package relationships • The two opposite exception arrows represent a confirmed source-level cycle',
             'A package can contain classes with different roles. Core GlobalExceptionHandler imports auth contracts; auth exceptions inherit the core base. No refactoring is applied.')
    p.group('presentation', 'Presentation & Contracts', 245, 135, 940, 220)
    p.group('business', 'Business Layer', 245, 480, 940, 210)
    p.group('domain', 'Business Entities', 1250, 135, 420, 555)
    p.group('cross', 'Cross-Cutting Concerns', 245, 825, 1425, 430)
    p.node('controller', 'auth.controller', ['RegistrationController', 'PasswordRecoveryController'], 275, 195, 350, 110, 'presentation', actual='auth.controller')
    p.node('dto', 'auth.dto', ['Request DTOs + internal state records', 'PreparedRegistrationResponse', 'AccountSnapshotResponse'], 805, 195, 350, 130, 'presentation', actual='auth.dto')
    p.node('service', 'auth.service', ['Registration / Password recovery'], 275, 540, 350, 110, 'business', actual='auth.service')
    p.node('accounts', 'user.service', ['AccountManagementService'], 805, 540, 350, 110, 'business', actual='user.service')
    p.node('user_entity', 'user.entity', ['User / Role'], 1280, 195, 360, 110, 'domain', actual='user.entity')
    p.node('candidate_entity', 'candidate.entity', ['Candidate'], 1280, 475, 360, 110, 'domain', actual='candidate.entity')
    p.node('auth_exception', 'auth.exception', ['RegistrationFlowException', 'PasswordRecoveryFlowException', 'Delivery / unavailable exceptions'], 275, 900, 350, 150, 'cross', actual='auth.exception')
    p.node('core_exception', 'core.exception', ['BaseBusinessException', 'GlobalExceptionHandler', 'Imports auth DTOs + exceptions'], 805, 900, 350, 150, 'cross', actual='core.exception')
    p.node('validation', 'core.validation', ['PasswordMatches', 'PasswordMatchesValidator'], 1280, 900, 360, 120, 'cross', actual='core.validation')
    p.node('user_exception', 'user.exception', ['AccountFieldException'], 805, 1130, 350, 90, 'cross', actual='user.exception')
    p.node('base', 'core.base', ['BaseEntity'], 1280, 1130, 360, 90, 'cross', actual='core.base')
    p.node('cycle_note', 'Confirmed package cycle', ['auth.exception ↔ core.exception', 'Inheritance dependency →', '← Handler imports / catches'], 25, 900, 190, 220, kind='note')
    p.edge('controller', 'dto', 'E21', 'form contracts', (1, .5), (0, .5))
    p.edge('controller', 'service', 'E01', 'injects')
    p.edge('service', 'accounts', 'E03', 'registration methods', (1, .5), (0, .5))
    p.edge('service', 'dto', 'E22', 'uses records', (0, .2), (0, .8), [(225, 562), (225, 385), (750, 385), (750, 299)])
    p.edge('service', 'auth_exception', 'E22', 'throws')
    p.edge('auth_exception', 'core_exception', 'E24', 'base type', (1, .25), (0, .25))
    p.edge('core_exception', 'auth_exception', 'E25', 'handler imports', (0, .8), (1, .8))
    p.edge('core_exception', 'dto', 'E26', 'rebuilds form DTOs', (1, .2), (.5, 1), [(1180, 930), (1180, 780), (1200, 780), (1200, 385), (980, 385)])
    p.edge('dto', 'validation', 'E23', '@PasswordMatches', (1, .5), (.5, 0), [(1230, 250), (1230, 855), (1460, 855)])
    p.edge('accounts', 'user_exception', 'E27', 'identifier conflict', (1, .5), (1, .5), [(1215, 595), (1215, 1175)])
    p.edge('user_exception', 'core_exception', 'E28', 'base type', (.5, 0), (.5, 1))
    p.edge('service', 'user_exception', 'E33', 'catches / translates', (.8, 1), (0, .5), [(555, 735), (710, 735), (710, 1175)])
    p.edge('candidate_entity', 'user_entity', 'E09', 'User account type', (.5, 0), (.5, 1))
    p.edge('user_entity', 'base', 'E29', 'base type', (1, .5), (1, .3), [(1700, 250), (1700, 1157)])
    p.edge('candidate_entity', 'base', 'E29', 'base type', (1, .5), (1, .8), [(1675, 530), (1675, 1202)])
    return p


def source_snapshot():
    digest = hashlib.sha256()
    files = sorted((ROOT / 'src').rglob('*')) + [ROOT / 'pom.xml']
    count = 0
    for path in files:
        if path.is_file():
            digest.update(path.relative_to(ROOT).as_posix().encode())
            digest.update(path.read_bytes())
            count += 1
    return {'file_count': count, 'sha256': digest.hexdigest()}


def xml_page(p):
    diagram = ET.Element('diagram', id=p.key, name=p.title)
    model = ET.SubElement(diagram, 'mxGraphModel', dx=str(W), dy=str(H), grid='1', gridSize='10',
                          guides='1', tooltips='1', connect='1', arrows='1', fold='1', page='1',
                          pageScale='1', pageWidth=str(W), pageHeight=str(H), background=BG, math='0', shadow='0')
    root = ET.SubElement(model, 'root')
    cid = lambda key: p.key + '_' + key
    ET.SubElement(root, 'mxCell', id=cid('root'))
    ET.SubElement(root, 'mxCell', id=cid('layer'), parent=cid('root'))
    nodes = {n['id']: n for n in p.nodes}
    def text_cell(key, text, x, y, w, h, size, bold=False):
        cell = ET.SubElement(root, 'mxCell', id=cid(key), value=text, vertex='1', parent=cid('layer'),
                             style=f'text;html=0;strokeColor=none;fillColor=none;whiteSpace=wrap;align=left;verticalAlign=middle;fontFamily=Arial;fontSize={size};fontColor={INK};fontStyle={1 if bold else 0};')
        ET.SubElement(cell, 'mxGeometry', x=str(x), y=str(y), width=str(w), height=str(h), attrib={'as': 'geometry'})
    text_cell('title', p.title, 25, 20, 1690, 43, 28, True)
    text_cell('subtitle', p.subtitle, 25, 68, 1690, 45, 17)
    text_cell('legend', 'Dashed open arrow: A depends on B.\nE##: source evidence index.\nNesting: logical architectural grouping.\nExternal actors are context only.\nThis is not a runtime sequence diagram.', 25, 1260, 430, 160, 16)
    text_cell('note', p.note, 25, 1570, 1690, 35, 14)
    for n in p.nodes:
        group = n['kind'] == 'group'
        value = '<b>' + html.escape(n['title']) + '</b>'
        if n['lines']:
            value += '<br><br>' + '<br>'.join(html.escape(line) for line in n['lines'])
        style = (f'shape=folder;tabWidth=70;tabHeight=20;html=1;whiteSpace=wrap;rounded=0;'
                 f'strokeColor={INK};strokeWidth=1.2;fillColor={GROUP if group else FILL};'
                 f'fontColor={INK};fontFamily=Arial;fontSize=16;'
                 f'align=center;verticalAlign=top;spacingTop=27;spacingLeft=9;spacingRight=9;'
                 f'container={1 if group else 0};collapsible=0;recursiveResize=0;')
        parent = nodes.get(n['parent'])
        cell = ET.SubElement(root, 'mxCell', id=cid(n['id']), value=value, style=style, vertex='1', parent=cid(n['parent'] or 'layer'))
        ET.SubElement(cell, 'mxGeometry', x=str(n['x']-(parent['x'] if parent else 0)),
                      y=str(n['y']-(parent['y'] if parent else 0)), width=str(n['w']), height=str(n['h']), attrib={'as': 'geometry'})
    for e in p.edges:
        sx, sy = e['exit']; tx, ty = e['entry']
        style = (f'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=0;'
                 f'endArrow=open;endFill=0;dashed=1;dashPattern=6 5;strokeColor={INK};strokeWidth=1.3;'
                 f'fontFamily=Arial;fontSize=13;fontColor={INK};labelBackgroundColor={BG};'
                 f'exitX={sx};exitY={sy};exitDx=0;exitDy=0;entryX={tx};entryY={ty};entryDx=0;entryDy=0;')
        cell = ET.SubElement(root, 'mxCell', id=e['id'], value=e['proof'],
                             style=style, edge='1', source=cid(e['source']), target=cid(e['target']), parent=cid('layer'))
        geo = ET.SubElement(cell, 'mxGeometry', relative='1', attrib={'as': 'geometry'})
        if e['points']:
            arr = ET.SubElement(geo, 'Array', attrib={'as': 'points'})
            for x, y in e['points']:
                ET.SubElement(arr, 'mxPoint', x=str(x), y=str(y))
    return diagram


def write_drawio(name, pages):
    path = OUT / name
    root = ET.Element('mxfile', host='app.diagrams.net', agent='RMS source-based architecture documentation', version='24.7.17', type='device')
    for p in pages:
        root.append(xml_page(p))
    ET.indent(root)
    ET.ElementTree(root).write(path, encoding='utf-8', xml_declaration=True)
    return path


FONT_DIR = Path('C:/Windows/Fonts')
def font(size, bold=False):
    return ImageFont.truetype(str(FONT_DIR / ('arialbd.ttf' if bold else 'arial.ttf')), size)


def render_xml(path):
    """Render the saved native cells as a transparent-to-review supporting PNG."""
    previews = []
    for diagram in ET.parse(path).getroot().findall('diagram'):
        model = diagram.find('mxGraphModel')
        cells = model.find('root').findall('mxCell')
        image = Image.new('RGB', (W, H), BG)
        draw = ImageDraw.Draw(image)
        edge_routes, text_bounds = [], []
        by_id = {c.get('id'): c for c in cells}
        boxes = {}
        def box(c):
            if c.get('id') in boxes:
                return boxes[c.get('id')]
            g = c.find('mxGeometry')
            x, y, w, h = [float(g.get(k, '0')) for k in ('x','y','width','height')]
            parent = by_id.get(c.get('parent'))
            if parent is not None and parent.find('mxGeometry') is not None:
                px, py, _, _ = box(parent); x += px; y += py
            boxes[c.get('id')] = (x,y,w,h)
            return x,y,w,h
        def style(c, name, default=None):
            m = re.search(r'(?:^|;)' + name + '=([^;]+)', c.get('style',''))
            return m.group(1) if m else default
        def lines_wrapped(text, max_width, f):
            result=[]
            for line in text.split('\n'):
                current=''
                for word in line.split(' '):
                    candidate=(current+' '+word).strip()
                    if draw.textlength(candidate,font=f)>max_width and current:
                        result.append(current); current=word
                    else: current=candidate
                result.append(current)
            return result
        for c in cells:
            if c.get('vertex') != '1': continue
            x,y,w,h = box(c)
            if 'shape=folder' in c.get('style',''):
                fill=style(c,'fillColor',FILL)
                draw.polygon([(x,y),(x+70,y),(x+70,y+20),(x+w,y+20),(x+w,y+h),(x,y+h)], fill=fill, outline=INK, width=1)
                draw.line([(x,y+20),(x+70,y+20)], fill=INK, width=1)
        for c in cells:
            if c.get('edge') != '1': continue
            a,b=box(by_id[c.get('source')]),box(by_id[c.get('target')])
            start=(a[0]+a[2]*float(style(c,'exitX','.5')),a[1]+a[3]*float(style(c,'exitY','1')))
            end=(b[0]+b[2]*float(style(c,'entryX','.5')),b[1]+b[3]*float(style(c,'entryY','0')))
            way=[(float(pt.get('x')),float(pt.get('y'))) for pt in c.findall('mxGeometry/Array/mxPoint')]
            pts=[start]+way+[end]
            orth=[pts[0]]
            for pt in pts[1:]:
                last=orth[-1]
                if abs(last[0]-pt[0])>.1 and abs(last[1]-pt[1])>.1:
                    orth.append((pt[0],last[1]))
                orth.append(pt)
            for a,b in zip(orth,orth[1:]):
                dx,dy=b[0]-a[0],b[1]-a[1]; length=math.hypot(dx,dy)
                if length==0: continue
                for t in range(0,math.ceil(length),11):
                    u,v=t/length,min(t+6,length)/length
                    draw.line([(a[0]+dx*u,a[1]+dy*u),(a[0]+dx*v,a[1]+dy*v)], fill=INK,width=2)
            edge_routes.append((c.get('id'), orth))
            prev=next((pt for pt in reversed(orth[:-1]) if pt!=end),start)
            angle=math.atan2(end[1]-prev[1],end[0]-prev[0])
            for delta in (-.5,.5):
                draw.line([end,(end[0]-12*math.cos(angle+delta),end[1]-12*math.sin(angle+delta))],fill=INK,width=2)
            segments=list(zip(orth,orth[1:]))
            # Longest segment keeps labels away from crowded package boundaries.
            a,b=max(segments,key=lambda seg:math.dist(*seg))
            tx,ty=(a[0]+b[0])/2,(a[1]+b[1])/2
            label=c.get('value'); f=font(13); tw=draw.textlength(label,font=f)
            draw.rectangle((tx-tw/2-4,ty-10,tx+tw/2+4,ty+9),fill=BG)
            draw.text((tx,ty),label,font=f,fill=INK,anchor='mm')
        for c in cells:
            if c.get('vertex') != '1': continue
            x,y,w,h=box(c); is_folder='shape=folder' in c.get('style','')
            raw=c.get('value',''); size=int(style(c,'fontSize','16'))
            if is_folder:
                parts=raw.split('<br><br>',1)
                title=html.unescape(re.sub('<[^>]+>','',parts[0]))
                body=html.unescape(re.sub('<[^>]+>','',parts[1].replace('<br>','\n'))) if len(parts)>1 else ''
                yy=y+28
                for i, block in enumerate([title,body] if body else [title]):
                    f=font(size,bold=(i==0))
                    wrapped=lines_wrapped(block,w-22,f)
                    for line in wrapped:
                        if line:
                            text_bounds.append((c.get('id'), draw.textbbox((x+w/2,yy),line,font=f,anchor='mt')))
                        draw.text((x+w/2,yy),line,font=f,fill=INK,anchor='mt'); yy+=size+4
                    if i==0 and body: yy+=7
                if yy>y+h+4:
                    RENDER_ISSUES.append(f'Label overflow: {diagram.get("id")} / {c.get("id")} {yy} > {y+h}')
            else:
                f=font(size,style(c,'fontStyle')=='1'); yy=y+3
                for line in lines_wrapped(raw,w,f):
                    draw.text((x,yy),line,font=f,fill=INK); yy+=size+4
        for edge_id, route in edge_routes:
            for node_id, (left, top, right, bottom) in text_bounds:
                for a,b in zip(route,route[1:]):
                    horizontal = abs(a[1]-b[1])<.01 and top<a[1]<bottom and max(min(a[0],b[0]),left)<min(max(a[0],b[0]),right)
                    vertical = abs(a[0]-b[0])<.01 and left<a[0]<right and max(min(a[1],b[1]),top)<min(max(a[1],b[1]),bottom)
                    if horizontal or vertical:
                        RENDER_ISSUES.append(f'Connector crosses text: {edge_id} / {node_id}')
        target=OUT / f'RMS_Authentication_{diagram.get("id")}.png'
        image.save(target); previews.append(target.name)
    return previews


def validate(path):
    pages=[]
    for diagram in ET.parse(path).getroot().findall('diagram'):
        cells=diagram.findall('mxGraphModel/root/mxCell'); ids=[c.get('id') for c in cells]
        assert len(ids)==len(set(ids)), 'Duplicate cell IDs'
        cell_ids=set(ids)
        for c in cells:
            assert c.get('parent') in cell_ids or c.get('parent') is None
            if c.get('edge')=='1':
                assert c.get('source') in cell_ids and c.get('target') in cell_ids
                assert 'dashed=1' in c.get('style') and 'endArrow=open' in c.get('style')
        pages.append({'page':diagram.get('name'),'cells':len(cells),'edges':sum(c.get('edge')=='1' for c in cells)})
    return pages


def main():
    before=source_snapshot()
    pages=[overview(),login(),otp_page(),otp_page(True),contracts()]
    # Check actual-package labels against source; logical/resource/framework nodes
    # deliberately have no actual field, and do not assert imaginary Java packages.
    java_sources = {f: f.read_text(encoding='utf-8-sig') for f in JAVA.rglob('*.java')}
    actual_packages={re.search(r'^package\s+([\w.]+);',body,re.M).group(1) for body in java_sources.values()}
    for p in pages:
        for n in p.nodes:
            assert 0<=n['x'] and n['x']+n['w']<=W and 0<=n['y'] and n['y']+n['h']<=H
            if n['actual']: assert 'com.group2.rms.'+n['actual'] in actual_packages
            if n['parent']:
                parent=next(x for x in p.nodes if x['id']==n['parent'])
                assert parent['x']<=n['x'] and n['x']+n['w']<=parent['x']+parent['w']
                assert parent['y']+55<=n['y'] and n['y']+n['h']<=parent['y']+parent['h']
        siblings=[n for n in p.nodes if n['kind']!='group']
        for i,a in enumerate(siblings):
            for b in siblings[i+1:]:
                assert not (max(a['x'],b['x'])<min(a['x']+a['w'],b['x']+b['w']) and
                            max(a['y'],b['y'])<min(a['y']+a['h'],b['y']+b['h'])), f'Overlapping nodes {p.key}/{a["id"]}/{b["id"]}'
    files=[write_drawio('RMS_Package_Diagram.drawio',pages[:1]),write_drawio('RMS_Detailed_Package_Diagram.drawio',pages[1:])]
    checks={f.name:validate(f) for f in files}
    previews=[name for f in files for name in render_xml(f)]
    assert not RENDER_ISSUES, '\n'.join(RENDER_ISSUES)
    after=source_snapshot(); assert before==after, 'Source changed during generation'
    selected = {'com.group2.rms.' + n['actual'] for p in pages for n in p.nodes if n['actual']}
    source_inventory = []
    for f, body in java_sources.items():
        package = re.search(r'^package\s+([\w.]+);',body,re.M).group(1)
        if package not in selected:
            continue
        imports = []
        for number, line in enumerate(body.splitlines(), 1):
            match = re.match(r'import\s+(?:static\s+)?([\w.*]+);',line)
            if match:
                imports.append({'import':match.group(1),'line':number})
        source_inventory.append({'file':f.relative_to(ROOT).as_posix(),'package':package,'imports':imports})
    inventory={'scope':'Authentication only; static source analysis', 'java_prefix':'com.group2.rms',
               'evidence_paths_relative_to':'src/main/java/com/group2/rms unless a root-relative path is specified',
               'proofs':PROOFS,'source_inventory':source_inventory,
               'inventory_note':'All imports of selected packages are listed; a class may also support non-authentication operations. Diagram arrows are the authentication scope selection.',
               'pages':[dict(key=p.key,title=p.title,nodes=p.nodes,edges=p.edges) for p in pages],
               'verification':{'xml':checks,'source_snapshot_before':before,'source_snapshot_after':after,
                               'node_bounds_and_sibling_overlap':'passed','actual_java_packages':'passed',
                               'preview_connector_text_intersections':'passed',
                               'native_diagrams_net_open':'not verified: browser ERR_BLOCKED_BY_CLIENT',
                               'preview_renderer':'Pillow rendering from saved mxGraph XML; not native diagrams.net',
                               'previews':previews}}
    (OUT/'RMS_Authentication_Dependency_Inventory.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
    index=['# Authentication diagram edge evidence','', 'Java paths below are relative to `src/main/java/com/group2/rms/` unless stated otherwise.','', '| ID | Directed relation / meaning | Source evidence |','|---|---|---|']
    for key,value in PROOFS.items():
        relations=set()
        for p in pages:
            titles={n['id']:n['title'] for n in p.nodes}
            for e in p.edges:
                if e['proof']==key:
                    relations.add(f"{titles[e['source']]} → {titles[e['target']]} ({e['label']})")
        index.append(f'| {key} | {"; ".join(sorted(relations))} | `{value}` |')
    (OUT/'RMS_Authentication_Edge_Evidence.md').write_text('\n'.join(index)+'\n',encoding='utf-8')
    print(json.dumps({'files':[str(f) for f in files],'verification':checks,'previews':previews,'source_unchanged':before==after},ensure_ascii=False,indent=2))


if __name__=='__main__':
    main()
