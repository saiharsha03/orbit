import { createRoot } from 'react-dom/client'
import '@fontsource-variable/inter'
import './assets/codicon.css'
import './themes.css'
import './styles.css'
import './shell.css'
import './conversation.css'
import './appearance'
import { Shell } from './Shell'
import { startEngine } from './engine'
import { TeamWindow, pickTeam } from './TeamView'
import { Companion } from './Companion'
import { teamWindowOf } from '../../shared/teamSidebarCore'

// ?view=team: the pop-out Team window, the Team view alone; ?view=companion: the small always-on-top companion;
// otherwise the whole app
const companion = new URLSearchParams(location.search).get('view') === 'companion'
const teamWin = companion ? null : teamWindowOf(location.search)
if (teamWin?.team) pickTeam(teamWin.team)
if (companion) document.documentElement.classList.add('companion')
else if (!teamWin) startEngine()
createRoot(document.getElementById('root')!).render(companion ? <Companion /> : teamWin ? <TeamWindow /> : <Shell />)
