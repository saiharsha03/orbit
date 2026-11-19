import { describe, expect, it } from 'vitest'
import { incomingLine, outgoingLine, toolVerb } from '../../src/renderer/src/conversationCore'

describe('thread messages as rows', () => {
  it('names the direction and the other thread', () => {
    expect(toolVerb('mcp__orbit__message_thread')).toEqual({ word: 'Outgoing', tone: 'agent' })
    expect(incomingLine('Fix login', '\n  Done, PR is up.\nMore here')).toBe('from Fix login: Done, PR is up.')
  })
  it('takes the receiver name from the answer, with the machine when it is linked', () => {
    expect(outgoingLine({ thread: 'new-1', text: 'Check this' }, 'Sent to thread "Fix login" (new-1).')).toBe('to Fix login: Check this')
    expect(outgoingLine({ thread: 'PC2/new-1', text: 'Hi' }, 'PC2: Queued for thread "Setup" (new-1); it goes in when its running turn ends.')).toBe('to Setup on PC2: Hi')
    expect(outgoingLine({ thread: 'new-9', text: 'Hi' })).toBe('to new-9: Hi')
  })
})
