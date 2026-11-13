export interface NoteTemplate {
  id: string;
  title: string;
  description: string;
  category: 'Work' | 'Personal' | 'Study' | 'Meeting' | 'Creative';
  icon: string;
  defaultTitle: string;
  defaultBody: string;
  defaultHtml: string;
  isChecklist: boolean;
  checklist?: { id: string; text: string; isDone: boolean }[];
  tags: string[];
}

export const NOTE_TEMPLATES: NoteTemplate[] = [
  {
    id: 'meeting-notes',
    title: 'Meeting Notes',
    description: 'Attendees, agenda, key decisions, and action items',
    category: 'Meeting',
    icon: 'Users',
    defaultTitle: 'Team Sync Meeting',
    defaultBody: `Meeting Overview\nDate: ${new Date().toLocaleDateString()}\nTime: 10:00 AM\nFacilitator:\nAttendees:\n\nObjectives\n1. Review milestones\n2. Confirm deliverables\n\nDiscussion & Key Points\n• \n• \n\nAction Items\n• Follow up with design\n• Schedule QA verification\n\nDecisions Made\n• `,
    defaultHtml: `<h2>Meeting Overview</h2><p><strong>Date:</strong> ${new Date().toLocaleDateString()}<br><strong>Time:</strong> 10:00 AM<br><strong>Facilitator:</strong> <br><strong>Attendees:</strong> </p><h3>Objectives</h3><ol><li>Review milestones</li><li>Confirm deliverables</li></ol><h3>Discussion &amp; Key Points</h3><ul><li></li></ul><h3>Action Items</h3><ul><li>Follow up with design</li><li>Schedule QA verification</li></ul><h3>Decisions Made</h3><ul><li></li></ul>`,
    isChecklist: false,
    tags: ['meeting', 'work'],
  },
  {
    id: 'project-plan',
    title: 'Project Roadmap',
    description: 'Milestones, deliverables, risks, and deadlines',
    category: 'Work',
    icon: 'Briefcase',
    defaultTitle: 'Project Blueprint',
    defaultBody: `Project Overview\nProject Name:\nLead:\nTarget Launch:\n\nCore Objectives\n1. Ship clean rich editing\n2. Support Home Screen widgets\n\nMilestones\nPhase 1: Planning — Wk 1 — Specifications (In Progress)\nPhase 2: Design — Wk 2 — Glass UI (Pending)\nPhase 3: Build — Wk 3 — Release (Pending)\n\nRisks & Mitigations\n• Risk:\n• Mitigation: `,
    defaultHtml: `<h2>Project Overview</h2><p><strong>Project Name:</strong> <br><strong>Lead:</strong> <br><strong>Target Launch:</strong> </p><h3>Core Objectives</h3><ol><li>Ship clean rich editing</li><li>Support Home Screen widgets</li></ol><h3>Milestones</h3><table><thead><tr><th>Phase</th><th>Target Date</th><th>Deliverable</th><th>Status</th></tr></thead><tbody><tr><td>Phase 1: Planning</td><td>Wk 1</td><td>Specifications</td><td>In Progress</td></tr><tr><td>Phase 2: Design</td><td>Wk 2</td><td>Glass UI</td><td>Pending</td></tr><tr><td>Phase 3: Build</td><td>Wk 3</td><td>Release</td><td>Pending</td></tr></tbody></table><h3>Risks &amp; Mitigations</h3><ul><li><strong>Risk:</strong> </li><li><strong>Mitigation:</strong> </li></ul>`,
    isChecklist: false,
    tags: ['project', 'roadmap'],
  },
  {
    id: 'daily-planner',
    title: 'Daily Checklist & Planner',
    description: 'Priorities, tasks, habits, and daily reflection',
    category: 'Personal',
    icon: 'CheckSquare',
    defaultTitle: `Daily Planner — ${new Date().toLocaleDateString()}`,
    defaultBody: `Top 3 Priorities for Today\n1. \n2. \n3. \n\nHealth & Habits\n• 8 Glasses of Water\n• 30 min Exercise or Walk\n• Read 15 pages\n\nEnd of Day Reflection\n• What went well today?\n• What can I improve tomorrow?`,
    defaultHtml: `<h3>Top 3 Priorities for Today</h3><ol><li></li><li></li><li></li></ol><h3>Health &amp; Habits</h3><ul><li>8 Glasses of Water</li><li>30 min Exercise or Walk</li><li>Read 15 pages</li></ul><h3>End of Day Reflection</h3><ul><li>What went well today?</li><li>What can I improve tomorrow?</li></ul>`,
    isChecklist: true,
    checklist: [
      { id: 'dp1', text: 'Top Priority 1', isDone: false },
      { id: 'dp2', text: 'Top Priority 2', isDone: false },
      { id: 'dp3', text: 'Top Priority 3', isDone: false },
      { id: 'dp4', text: 'Drink 2L of water', isDone: false },
      { id: 'dp5', text: 'Exercise 30 mins', isDone: false },
      { id: 'dp6', text: 'Evening review & winddown', isDone: false },
    ],
    tags: ['daily', 'habits'],
  },
  {
    id: 'cornell-study',
    title: 'Cornell Study Notes',
    description: 'Systematic lecture & book summary framework',
    category: 'Study',
    icon: 'GraduationCap',
    defaultTitle: 'Study Notes: Subject / Topic',
    defaultBody: `Topic:\nSource / Chapter:\nDate: ${new Date().toLocaleDateString()}\n\nCue Questions / Keywords\n• Key Term 1:\n• Core Question 2:\n\nLecture Notes & Definitions\n• Definition:\n• Formula / Concept:\n\n"Important quote or rule from reading"\n\nSummary (in your own words)\nIn brief: `,
    defaultHtml: `<h2>Topic</h2><p><strong>Source / Chapter:</strong> <br><strong>Date:</strong> ${new Date().toLocaleDateString()}</p><h3>Cue Questions / Keywords</h3><ul><li>Key Term 1: </li><li>Core Question 2: </li></ul><h3>Lecture Notes &amp; Definitions</h3><ul><li>Definition: </li><li>Formula / Concept: </li></ul><blockquote>"Important quote or rule from reading"</blockquote><h3>Summary (in your own words)</h3><p>In brief: </p>`,
    isChecklist: false,
    tags: ['study', 'learning'],
  },
  {
    id: 'creative-brainstorm',
    title: 'Brainstorm & Mindmap',
    description: 'Idea generation, pros/cons, and creative sprint',
    category: 'Creative',
    icon: 'Sparkles',
    defaultTitle: 'Idea Sprint: New Concept',
    defaultBody: `The Core Problem\nWhat are we solving?\n\nBrainstormed Angles\n• Idea A:\n• Idea B:\n• Idea C:\n\nImmediate Next Experiment\n• Step 1: `,
    defaultHtml: `<h2>The Core Problem</h2><p>What are we solving?</p><h3>Brainstormed Angles</h3><ul><li>Idea A: </li><li>Idea B: </li><li>Idea C: </li></ul><h3>Pros vs. Cons Analysis</h3><table><thead><tr><th>Option</th><th>Pros</th><th>Cons</th></tr></thead><tbody><tr><td>Option 1</td><td>Quick to test</td><td>Limited scale</td></tr><tr><td>Option 2</td><td>High impact</td><td>Requires setup</td></tr></tbody></table><h3>Immediate Next Experiment</h3><ul><li>Step 1: </li></ul>`,
    isChecklist: false,
    tags: ['brainstorm', 'creative'],
  },
];
